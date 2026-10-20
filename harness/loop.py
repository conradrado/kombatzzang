"""Planner -> (Generator -> Verify -> Evaluator)* 루프."""
import hashlib
import json
import re
from pathlib import Path

import guard
from verify import run_verify

# 하네스 실행 상태 값. state["status"] 에 저장된다.
SUCCESS, ABORTED, RUNNING = "SUCCESS", "ABORTED", "RUNNING"


class Harness:
    """Planner -> Generator -> 테스트 -> Evaluator 를 반복하는 상태 머신.

    cfg: config.toml 내용, agents: plan/generate/evaluate 를 가진 객체(테스트에서는 가짜로 대체),
    repo: 대상 git 저장소, run_dir: 이번 실행의 산출물(state.json, plan.json 등) 저장 폴더,
    verifier: 테스트 실행 함수, log: 진행 로그 출력 함수.
    """

    def __init__(self, cfg, agents, repo, run_dir, verifier=run_verify, log=print):
        self.cfg, self.agents, self.repo = cfg, agents, repo
        self.run_dir = Path(run_dir)
        self.verifier, self.log = verifier, log
        self.state_path = self.run_dir / "state.json"

    # ---- 상태 저장/복원 -------------------------------------------------
    def new_state(self, goal):
        """새 실행의 초기 상태. baseline 은 시작 시점에 이미 변경돼 있던 파일들의 기준선이다."""
        return {
            "goal": goal, "status": RUNNING, "plan": None, "iteration": 0,
            "replans": 0, "cost_usd": 0.0, "feedback": "", "last_signature": None,
            "baseline": guard.snapshot(self.repo), "reason": "",
        }

    def save(self, state):
        """상태를 state.json 에 저장한다. 중단돼도 --resume 으로 이어갈 수 있게 하는 지점이다."""
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.state_path.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")

    def load(self):
        """저장된 state.json 을 읽어 온다. (--resume 용)"""
        return json.loads(self.state_path.read_text(encoding="utf-8"))

    def _dump(self, name, data):
        """run 폴더에 산출물 파일을 쓴다. 문자열은 그대로, 그 외는 JSON 으로 저장한다."""
        (self.run_dir / name).write_text(
            data if isinstance(data, str) else json.dumps(data, ensure_ascii=False, indent=2),
            encoding="utf-8")

    # ---- 메인 루프 ------------------------------------------------------
    def run(self, state):
        """루프 전체를 실행하고 최종 state 를 반환한다. 종료 조건: 성공 / 비용 상한 / replan 한도 / 최대 반복."""
        limits = self.cfg["limits"]
        (self.run_dir / "goal.md").parent.mkdir(parents=True, exist_ok=True)
        self._dump("goal.md", state["goal"])
        try:
            # 새 실행이면 계획부터 세운다. (--resume 은 저장된 plan 을 그대로 사용)
            if state["plan"] is None:
                self._plan(state)
            while state["iteration"] < limits["max_iterations"]:
                # 매 반복 시작 전에 누적 비용을 확인한다.
                if state["cost_usd"] >= limits["max_total_cost_usd"]:
                    return self._finish(state, ABORTED, f"비용 상한 초과 (${state['cost_usd']:.2f})")
                state["iteration"] += 1
                self.log(f"\n=== iteration {state['iteration']}/{limits['max_iterations']} ===")
                result = self._iterate(state)
                if result["success"]:
                    return self._finish(state, SUCCESS, "테스트 green + Evaluator PASS")
                # 직전과 같은 이유로 또 실패했다면 Generator 반복이 의미 없으므로 계획을 다시 세운다.
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
        """최종 상태를 기록하고 result.json 요약을 남긴 뒤 state 를 반환한다."""
        state["status"], state["reason"] = status, reason
        self.save(state)
        self._dump("result.json", {k: state[k] for k in
                                   ("status", "reason", "iteration", "replans", "cost_usd")})
        self.log(f"\n[{status}] {reason} (iterations={state['iteration']}, cost=${state['cost_usd']:.2f})")
        return state

    def _plan(self, state, replan_reason=None):
        """Planner 를 호출해 state["plan"] 을 갱신한다. replan_reason 이 있으면 이전 계획과 실패 요약을 함께 보낸다."""
        self.log("Planner 호출" + (" (replan)" if replan_reason else ""))
        prompt = f"## 목표\n{state['goal']}\n"
        if replan_reason:
            prompt += (f"\n## 이전 계획\n{json.dumps(state['plan'], ensure_ascii=False, indent=2)}\n"
                       f"\n## 이전 시도 실패 요약 (계획을 고쳐 다시 세울 것)\n{replan_reason}\n")
        plan, cost = self.agents.plan(prompt)
        state["cost_usd"] += cost
        state["plan"] = plan
        state["last_signature"] = None  # 새 계획이므로 정체(stagnation) 판정 기준을 초기화
        self._dump("plan.json" if not replan_reason else f"plan.replan{state['replans']}.json", plan)
        self.save(state)

    def _iterate(self, state):
        """한 번의 반복(Generator -> 범위 검사 -> 테스트 -> Evaluator)을 수행하고 {"success", "stagnant"} 를 반환한다."""
        n, plan = state["iteration"], state["plan"]
        it_dir = self.run_dir / f"iter-{n:02d}"  # 반복별 산출물 폴더
        it_dir.mkdir(parents=True, exist_ok=True)

        # 1) Generator: 허용 경로를 훅에도 넘겨 범위 밖 쓰기를 호출 시점에 막는다.
        self.log("Generator 호출")
        gen, cost = self.agents.generate(self._generator_prompt(state),
                                         allowed_paths=plan["allowed_paths"])
        state["cost_usd"] += cost
        (it_dir / "generator.json").write_text(json.dumps(gen, ensure_ascii=False, indent=2), encoding="utf-8")

        # 2) 범위 검사: 훅이 놓친 변경을 사후에 잡아 되돌리는 안전망. 범위 안 변경만 diff 로 만든다.
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

        # 성공 조건: 테스트 통과 + Evaluator PASS + 모든 기준 충족 + 범위 위반 없음 (모두 만족해야 함)
        unmet = self._unmet_criteria(plan, ev)
        success = vr.passed and ev["verdict"] == "PASS" and not unmet and not violations
        self.log(f"판정: {ev['verdict']} (미충족 기준 {len(unmet)}개, 범위 위반 {len(violations)}건)")
        if success:
            return {"success": True, "stagnant": False}

        # 실패 시: 직전 반복과 실패 지문이 같으면 정체(stagnant)로 판단하고, 다음 반복에 줄 피드백을 만든다.
        sig = self._signature(vr, ev, violations)
        stagnant = sig == state["last_signature"]
        state["last_signature"] = sig
        state["feedback"] = self._compose_feedback(ev, vr, unmet, violations, unreverted)
        return {"success": False, "stagnant": stagnant}

    # ---- 프롬프트/피드백 조립 ---------------------------------------------
    def _generator_prompt(self, state):
        """Generator 입력: 목표 + 계획 (+ 직전 반복의 피드백이 있으면 포함)."""
        p = f"## 목표\n{state['goal']}\n\n## 계획\n{json.dumps(state['plan'], ensure_ascii=False, indent=2)}\n"
        if state["feedback"]:
            p += f"\n## 직전 반복의 피드백 (반드시 해결할 것)\n{state['feedback']}\n"
        return p

    def _evaluator_prompt(self, state, gen, diff, vr, violations):
        """Evaluator 입력: 목표, acceptance criteria, Generator 보고, 테스트 결과, 범위 위반, 변경 diff."""
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
        """계획의 acceptance criteria 중 Evaluator 가 충족(met)으로 판정하지 않은(누락 포함) id 목록."""
        met = {c["id"]: c["met"] for c in ev["criteria"]}
        return [c["id"] for c in plan["acceptance_criteria"] if not met.get(c["id"], False)]

    @staticmethod
    def _signature(vr, ev, violations):
        """실패 지문(해시). 숫자(줄 번호, 시간 등)를 '#' 로 치환해 같은 실패가 반복되는지 비교할 수 있게 한다."""
        lines = [re.sub(r"\d+", "#", l) for l in vr.failure_lines]
        issues = [re.sub(r"\d+", "#", i) for i in ev.get("blocking_issues", [])]
        raw = json.dumps([vr.passed, lines, issues, sorted(violations)], ensure_ascii=False)
        return hashlib.sha1(raw.encode()).hexdigest()

    @staticmethod
    def _compose_feedback(ev, vr, unmet, violations, unreverted):
        """다음 반복의 Generator 에게 줄 피드백 문자열을 조립한다. (Evaluator 의견, 차단 이슈, 테스트 로그, 범위 위반 등)"""
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
