"""Planner -> (Generator -> Verify -> Evaluator)* 루프."""
import hashlib
import json
import re
from pathlib import Path

import guard
from verify import run_verify

SUCCESS, ABORTED, RUNNING = "SUCCESS", "ABORTED", "RUNNING"


class Harness:
    def __init__(self, cfg, agents, repo, run_dir, verifier=run_verify, log=print):
        self.cfg, self.agents, self.repo = cfg, agents, repo
        self.run_dir = Path(run_dir)
        self.verifier, self.log = verifier, log
        self.state_path = self.run_dir / "state.json"

    # ---- 상태 저장/복원 -------------------------------------------------
    def new_state(self, goal):
        return {
            "goal": goal, "status": RUNNING, "plan": None, "iteration": 0,
            "replans": 0, "cost_usd": 0.0, "feedback": "", "last_signature": None,
            "baseline": guard.snapshot(self.repo), "reason": "",
        }

    def save(self, state):
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.state_path.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")

    def load(self):
        return json.loads(self.state_path.read_text(encoding="utf-8"))

    def _dump(self, name, data):
        (self.run_dir / name).write_text(
            data if isinstance(data, str) else json.dumps(data, ensure_ascii=False, indent=2),
            encoding="utf-8")

    # ---- 메인 루프 ------------------------------------------------------
    def run(self, state):
        limits = self.cfg["limits"]
        (self.run_dir / "goal.md").parent.mkdir(parents=True, exist_ok=True)
        self._dump("goal.md", state["goal"])
        try:
            if state["plan"] is None:
                self._plan(state)
            while state["iteration"] < limits["max_iterations"]:
                if state["cost_usd"] >= limits["max_total_cost_usd"]:
                    return self._finish(state, ABORTED, f"비용 상한 초과 (${state['cost_usd']:.2f})")
                state["iteration"] += 1
                self.log(f"\n=== iteration {state['iteration']}/{limits['max_iterations']} ===")
                result = self._iterate(state)
                if result["success"]:
                    return self._finish(state, SUCCESS, "테스트 green + Evaluator PASS")
                if result["stagnant"]:
                    if state["replans"] >= limits["max_replans"]:
                        return self._finish(state, ABORTED, "같은 실패가 반복되고 replan 한도 소진")
                    state["replans"] += 1
                    self.log("같은 실패 반복 -> Planner 재호출")
                    self._plan(state, replan_reason=state["feedback"])
                self.save(state)
            return self._finish(state, ABORTED, f"최대 반복 {limits['max_iterations']}회 도달")
        except Exception as e:  # 에이전트/CLI 오류도 상태를 남기고 종료 (--resume 가능)
            state["reason"] = f"{type(e).__name__}: {e}"
            self.save(state)
            raise

    def _finish(self, state, status, reason):
        state["status"], state["reason"] = status, reason
        self.save(state)
        self._dump("result.json", {k: state[k] for k in
                                   ("status", "reason", "iteration", "replans", "cost_usd")})
        self.log(f"\n[{status}] {reason} (iterations={state['iteration']}, cost=${state['cost_usd']:.2f})")
        return state

    def _plan(self, state, replan_reason=None):
        self.log("Planner 호출" + (" (replan)" if replan_reason else ""))
        prompt = f"## 목표\n{state['goal']}\n"
        if replan_reason:
            prompt += (f"\n## 이전 계획\n{json.dumps(state['plan'], ensure_ascii=False, indent=2)}\n"
                       f"\n## 이전 시도 실패 요약 (계획을 고쳐 다시 세울 것)\n{replan_reason}\n")
        plan, cost = self.agents.plan(prompt)
        state["cost_usd"] += cost
        state["plan"] = plan
        state["last_signature"] = None
        self._dump("plan.json" if not replan_reason else f"plan.replan{state['replans']}.json", plan)
        self.save(state)

    def _iterate(self, state):
        n, plan = state["iteration"], state["plan"]
        it_dir = self.run_dir / f"iter-{n:02d}"
        it_dir.mkdir(parents=True, exist_ok=True)

        # 1) Generator
        self.log("Generator 호출")
        gen, cost = self.agents.generate(self._generator_prompt(state))
        state["cost_usd"] += cost
        (it_dir / "generator.json").write_text(json.dumps(gen, ensure_ascii=False, indent=2), encoding="utf-8")

        # 2) 범위 검사
        changed = guard.changed_since(state["baseline"], guard.snapshot(self.repo))
        in_scope, violations = guard.split_violations(changed, plan["allowed_paths"])
        reverted, unreverted = guard.revert(self.repo, violations, state["baseline"]) if violations else ([], [])
        if violations:
            self.log(f"범위 밖 변경 {len(violations)}건 (되돌림 {len(reverted)}, 못 되돌림 {len(unreverted)})")
        diff = guard.build_diff(self.repo, in_scope)
        (it_dir / "diff.patch").write_text(diff, encoding="utf-8")

        # 3) 테스트 (하네스가 직접)
        self.log("테스트 실행")
        vr = self.verifier(self.cfg, plan, self.repo, it_dir / "verify.log")
        self.log(f"테스트 {'통과' if vr.passed else '실패'} (exit={vr.exit_code})")

        # 4) Evaluator
        self.log("Evaluator 호출")
        ev, cost = self.agents.evaluate(self._evaluator_prompt(state, gen, diff, vr, violations))
        state["cost_usd"] += cost
        (it_dir / "evaluation.json").write_text(json.dumps(ev, ensure_ascii=False, indent=2), encoding="utf-8")

        unmet = self._unmet_criteria(plan, ev)
        success = vr.passed and ev["verdict"] == "PASS" and not unmet and not violations
        self.log(f"판정: {ev['verdict']} (미충족 기준 {len(unmet)}개, 범위 위반 {len(violations)}건)")
        if success:
            return {"success": True, "stagnant": False}

        sig = self._signature(vr, ev, violations)
        stagnant = sig == state["last_signature"]
        state["last_signature"] = sig
        state["feedback"] = self._compose_feedback(ev, vr, unmet, violations, unreverted)
        return {"success": False, "stagnant": stagnant}

    # ---- 프롬프트/피드백 조립 ---------------------------------------------
    def _generator_prompt(self, state):
        p = f"## 목표\n{state['goal']}\n\n## 계획\n{json.dumps(state['plan'], ensure_ascii=False, indent=2)}\n"
        if state["feedback"]:
            p += f"\n## 직전 반복의 피드백 (반드시 해결할 것)\n{state['feedback']}\n"
        return p

    def _evaluator_prompt(self, state, gen, diff, vr, violations):
        return (
            f"## 목표\n{state['goal']}\n\n"
            f"## Acceptance Criteria\n{json.dumps(state['plan']['acceptance_criteria'], ensure_ascii=False, indent=2)}\n\n"
            f"## Generator 보고\n{json.dumps(gen, ensure_ascii=False, indent=2)}\n\n"
            f"## 테스트 결과: {'통과' if vr.passed else '실패'} (exit={vr.exit_code}, 시간초과={vr.timed_out})\n"
            f"```\n{vr.log_tail}\n```\n\n"
            f"## 범위 위반 (되돌려진 변경)\n{violations or '없음'}\n\n"
            f"## 변경 diff\n```diff\n{diff}\n```\n"
        )

    @staticmethod
    def _unmet_criteria(plan, ev):
        met = {c["id"]: c["met"] for c in ev["criteria"]}
        return [c["id"] for c in plan["acceptance_criteria"] if not met.get(c["id"], False)]

    @staticmethod
    def _signature(vr, ev, violations):
        lines = [re.sub(r"\d+", "#", l) for l in vr.failure_lines]
        issues = [re.sub(r"\d+", "#", i) for i in ev.get("blocking_issues", [])]
        raw = json.dumps([vr.passed, lines, issues, sorted(violations)], ensure_ascii=False)
        return hashlib.sha1(raw.encode()).hexdigest()

    @staticmethod
    def _compose_feedback(ev, vr, unmet, violations, unreverted):
        parts = [f"Evaluator 피드백:\n{ev['feedback']}"]
        if ev.get("blocking_issues"):
            parts.append("차단 이슈:\n" + "\n".join(f"- {i}" for i in ev["blocking_issues"]))
        if unmet:
            parts.append(f"미충족 acceptance criteria: {', '.join(unmet)}")
        if not vr.passed:
            parts.append(f"테스트 실패 로그(끝부분):\n{vr.log_tail}")
        if violations:
            parts.append("허용 경로 밖 변경은 되돌려졌다. 다시 만들지 말고 허용 경로 안에서만 작업할 것: "
                         + ", ".join(violations))
        if unreverted:
            parts.append("되돌리지 못한 범위 밖 변경(하네스 시작 전부터 더러웠던 파일): " + ", ".join(unreverted))
        return "\n\n".join(parts)
