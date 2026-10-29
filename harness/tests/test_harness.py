"""가짜 에이전트 + 임시 git 저장소로 루프/가드 동작을 검증한다. (claude CLI, gradle 불필요)

실행: python harness/tests/test_harness.py
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import branching
import guard
import integrate
import parallel
import publish
from config import load_config
from loop import ABORTED, SUCCESS, Harness
from schemas import SchemaError, validate, PLAN_SCHEMA
from verify import VerifyResult

PLAN = {
    "summary": "s",
    "tasks": [{"id": "T1", "title": "t", "description": "d"}],
    "acceptance_criteria": [{"id": "C1", "description": "c"}],
    "allowed_paths": ["src/main/**"],
}


def sh(repo, *args):
    subprocess.run(args, cwd=repo, check=True, capture_output=True)


def make_repo(tmp):
    repo = Path(tmp)
    sh(repo, "git", "init", "-q")
    sh(repo, "git", "config", "user.email", "t@t")
    sh(repo, "git", "config", "user.name", "t")
    (repo / "src/main").mkdir(parents=True)
    (repo / "README.md").write_text("hi", encoding="utf-8")
    sh(repo, "git", "add", ".")
    sh(repo, "git", "commit", "-qm", "init")
    return repo


class FakeAgents:
    def __init__(self, repo, generate_fn, verdicts):
        self.repo, self.generate_fn, self.verdicts = repo, generate_fn, list(verdicts)
        self.plans = 0
        self.gen_calls = 0

    def plan(self, prompt):
        self.plans += 1
        return dict(PLAN), 0.1

    def generate(self, prompt, allowed_paths=None):
        self.gen_calls += 1
        self.generate_fn(self.repo, self.gen_calls)
        return {"summary": "x", "changed_files": []}, 0.1

    def evaluate(self, prompt):
        verdict = self.verdicts.pop(0) if self.verdicts else "FAIL"
        return {"verdict": verdict, "criteria": [{"id": "C1", "met": verdict == "PASS", "evidence": "e"}],
                "feedback": "fix it", "blocking_issues": ["same issue"]}, 0.1


def verifier_from(results):
    results = list(results)

    def run(cfg, plan, repo, log_path):
        ok = results.pop(0) if results else False
        return VerifyResult(ok, 0 if ok else 1, False, "tail", [] if ok else ["Foo FAILED"])
    return run


class HarnessTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = make_repo(self.tmp.name)
        self.cfg = load_config()
        self.cfg["limits"]["max_iterations"] = 4

    def tearDown(self):
        self.tmp.cleanup()

    def harness(self, agents, verifier):
        return Harness(self.cfg, agents, self.repo, self.repo.parent / (self.repo.name + "-run"),
                       verifier=verifier, log=lambda *_: None)

    def test_success_on_second_iteration(self):
        def gen(repo, n):
            (repo / "src/main/A.java").write_text(f"v{n}", encoding="utf-8")
        agents = FakeAgents(self.repo, gen, ["FAIL", "PASS"])
        h = self.harness(agents, verifier_from([False, True]))
        state = h.run(h.new_state("goal"))
        self.assertEqual(state["status"], SUCCESS)
        self.assertEqual(state["iteration"], 2)

    def test_pass_verdict_with_failing_tests_is_not_success(self):
        agents = FakeAgents(self.repo, lambda r, n: None, ["PASS"] * 4)
        h = self.harness(agents, verifier_from([False] * 4))
        state = h.run(h.new_state("goal"))
        self.assertEqual(state["status"], ABORTED)

    def test_stagnation_triggers_replan_then_abort(self):
        agents = FakeAgents(self.repo, lambda r, n: None, [])
        h = self.harness(agents, verifier_from([]))
        state = h.run(h.new_state("goal"))
        self.assertEqual(state["status"], ABORTED)
        self.assertEqual(agents.plans, 2)  # 최초 계획 + replan 1회
        self.assertIn("replan", state["reason"])

    def test_out_of_scope_change_is_reverted_and_blocks_success(self):
        def gen(repo, n):
            (repo / "src/main/A.java").write_text("ok", encoding="utf-8")
            (repo / "README.md").write_text("tampered", encoding="utf-8")
            (repo / "stray.txt").write_text("x", encoding="utf-8")
        agents = FakeAgents(self.repo, gen, ["PASS"])
        self.cfg["limits"]["max_iterations"] = 1
        h = self.harness(agents, verifier_from([True]))
        state = h.run(h.new_state("goal"))
        self.assertEqual(state["status"], ABORTED)
        self.assertEqual((self.repo / "README.md").read_text(encoding="utf-8"), "hi")
        self.assertFalse((self.repo / "stray.txt").exists())
        self.assertTrue((self.repo / "src/main/A.java").exists())

    def test_cost_limit_aborts(self):
        self.cfg["limits"]["max_total_cost_usd"] = 0.25
        agents = FakeAgents(self.repo, lambda r, n: None, [])
        h = self.harness(agents, verifier_from([]))
        state = h.run(h.new_state("goal"))
        self.assertEqual(state["status"], ABORTED)
        self.assertIn("비용", state["reason"])


class GuardTest(unittest.TestCase):
    def test_in_scope(self):
        allowed = ["src/main/java/**/post/**", "src/test/java/**/post/**", "build.gradle"]
        self.assertTrue(guard.in_scope("src/main/java/com/x/post/Post.java", allowed))
        self.assertFalse(guard.in_scope("src/main/java/com/x/user/User.java", allowed))
        self.assertTrue(guard.in_scope("build.gradle", allowed))
        self.assertFalse(guard.in_scope("harness/run.py", ["**"]))
        self.assertFalse(guard.in_scope(".github/ISSUE_TEMPLATE/a.md", ["**"]))

    def test_snapshot_ignores_preexisting_dirty_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = make_repo(tmp)
            (repo / "dirty.txt").write_text("1", encoding="utf-8")
            base = guard.snapshot(repo)
            self.assertEqual(guard.changed_since(base, guard.snapshot(repo)), [])
            (repo / "dirty.txt").write_text("2", encoding="utf-8")
            self.assertEqual(guard.changed_since(base, guard.snapshot(repo)), ["dirty.txt"])


class ScopeGuardHookTest(unittest.TestCase):
    """PreToolUse 훅: stdin 이벤트 -> 거부 사유(None 이면 통과)."""
    HOOK = Path(__file__).resolve().parents[1] / "hooks" / "scope_guard.py"

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name).resolve()
        self.scope = {"repo_root": str(self.root), "allowed_paths": ["src/main/**"]}

    def tearDown(self):
        self.tmp.cleanup()

    def decide(self, tool, **tool_input):
        sys.path.insert(0, str(self.HOOK.parent))
        import scope_guard
        return scope_guard.decide({"tool_name": tool, "tool_input": tool_input}, self.scope)

    def test_write_inside_scope_allowed(self):
        self.assertIsNone(self.decide("Edit", file_path=str(self.root / "src/main/A.java")))
        self.assertIsNone(self.decide("Write", file_path="src/main/A.java"))  # 상대 경로

    def test_write_outside_scope_denied(self):
        self.assertIn("허용 경로 밖", self.decide("Write", file_path=str(self.root / "README.md")))

    def test_always_forbidden_denied_even_if_allowed(self):
        self.scope["allowed_paths"] = ["**"]
        self.assertIsNotNone(self.decide("Edit", file_path=str(self.root / "harness/run.py")))
        self.assertIsNotNone(self.decide("Edit", file_path=str(self.root / ".claude/x.md")))

    def test_path_escaping_repo_denied(self):
        self.assertIn("저장소 밖", self.decide("Edit", file_path=str(self.root / "src/main/../../../etc/x")))
        self.assertIsNotNone(self.decide("Edit", file_path=str(self.root.parent / "other.txt")))

    def test_bash_writes_denied_reads_allowed(self):
        self.assertIsNotNone(self.decide("Bash", command="echo hi > README.md"))
        self.assertIsNotNone(self.decide("Bash", command="./gradlew test && sed -i s/a/b/ x"))
        self.assertIsNotNone(self.decide("Bash", command="cp a b"))
        self.assertIsNone(self.decide("Bash", command="./gradlew test 2>&1"))
        self.assertIsNone(self.decide("Bash", command="git diff > /dev/null"))
        self.assertIsNone(self.decide("Bash", command="git status"))

    def test_other_tools_pass(self):
        self.assertIsNone(self.decide("Read", file_path=str(self.root / "README.md")))

    def test_process_denies_via_json_and_passes_without_env(self):
        event = json.dumps({"tool_name": "Write", "tool_input": {"file_path": str(self.root / "x.txt")}})
        env = {**os.environ, "HARNESS_SCOPE": json.dumps(self.scope)}
        out = subprocess.run([sys.executable, str(self.HOOK)], input=event, env=env,
                             capture_output=True, text=True, encoding="utf-8").stdout
        self.assertEqual(json.loads(out)["hookSpecificOutput"]["permissionDecision"], "deny")
        env.pop("HARNESS_SCOPE")
        out = subprocess.run([sys.executable, str(self.HOOK)], input=event, env=env,
                             capture_output=True, text=True, encoding="utf-8").stdout
        self.assertEqual(out, "")

    def test_process_handles_korean_path_regardless_of_system_encoding(self):
        event = json.dumps({"tool_name": "Write", "tool_input": {"file_path": str(self.root / "한글 이슈.txt")}},
                           ensure_ascii=False).encode("utf-8")
        env = {**os.environ, "HARNESS_SCOPE": json.dumps(self.scope)}
        env.pop("PYTHONUTF8", None)
        env.pop("PYTHONIOENCODING", None)
        proc = subprocess.run([sys.executable, "-X", "utf8=0", str(self.HOOK)], input=event, env=env, capture_output=True)
        self.assertEqual(proc.returncode, 0, proc.stderr.decode("utf-8", "replace"))
        self.assertIn("한글 이슈.txt", json.loads(proc.stdout.decode("utf-8"))["hookSpecificOutput"]["permissionDecisionReason"])


class BranchingTest(unittest.TestCase):
    ISSUES = [{"number": 18, "title": "[Feat] 게시글 - 예외 처리"}, {"number": 77, "title": "[Feat] 랜딩"}]
    BRANCHES = ["feature/18-post-exception", "feature/6-user-controller"]

    def test_branch_match(self):
        r = {"match": "branch", "reason": "r", "branch": "feature/18-post-exception"}
        self.assertEqual(branching.decide(r, self.ISSUES, self.BRANCHES), ("checkout", "feature/18-post-exception"))

    def test_rejects_unknown_branch_or_issue(self):
        with self.assertRaises(branching.BranchError):
            branching.decide({"match": "branch", "reason": "r", "branch": "feature/99-x"}, self.ISSUES, self.BRANCHES)
        with self.assertRaises(branching.BranchError):
            branching.decide({"match": "issue", "reason": "r", "issue_number": 99}, self.ISSUES, self.BRANCHES)

    def test_issue_reuses_existing_branch_else_creates(self):
        r = {"match": "issue", "reason": "r", "issue_number": 18, "slug": "x"}
        self.assertEqual(branching.decide(r, self.ISSUES, self.BRANCHES), ("checkout", "feature/18-post-exception"))
        r = {"match": "issue", "reason": "r", "issue_number": 77, "slug": "Landing Page!"}
        self.assertEqual(branching.decide(r, self.ISSUES, self.BRANCHES),
                         ("create_branch", 77, "feature/77-landing-page"))

    def test_none_requires_title_and_tasks(self):
        with self.assertRaises(branching.BranchError):
            branching.decide({"match": "none", "reason": "r"}, self.ISSUES, self.BRANCHES)
        r = {"match": "none", "reason": "r", "title": "[Feat] t", "tasks": ["a"]}
        self.assertEqual(branching.decide(r, self.ISSUES, self.BRANCHES)[0], "create_issue")
        self.assertIn("- [ ] a", branching.issue_body(r))

    def test_checkout_creates_from_base_and_switches(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = make_repo(tmp)
            sh(repo, "git", "branch", "-M", "dev")
            branching.checkout(repo, "feature/1-a", "dev")
            self.assertEqual(guard.current_branch(repo), "feature/1-a")
            sh(repo, "git", "checkout", "-q", "dev")
            branching.checkout(repo, "feature/1-a", "dev")  # 이미 있으면 이동만
            self.assertEqual(guard.current_branch(repo), "feature/1-a")
            self.assertEqual(branching.list_branches(repo), ["feature/1-a"])


class PublishTest(unittest.TestCase):
    FILES = ["src/main/A.java", "src/main/B.java", "src/main/C.java"]

    def test_check_commits_requires_each_file_exactly_once(self):
        ok = [{"files": ["src/main/A.java", "src/main/B.java"]}, {"files": ["src/main/C.java"]}]
        publish.check_commits(ok, self.FILES)
        for bad in ([{"files": ["src/main/A.java"]}],  # 누락
                    ok + [{"files": ["src/main/A.java"]}],  # 중복
                    ok + [{"files": ["x.java"]}],  # 목록에 없는 파일
                    ok + [{"files": []}], []):
            with self.assertRaises(publish.PublishError):
                publish.check_commits(bad, self.FILES)

    def test_commit_message_and_issue(self):
        self.assertEqual(publish.issue_of("feature/83-user-region-update"), 83)
        self.assertIsNone(publish.issue_of("chore/x"))
        c = {"type": "Feat", "subject": " 유저 - 지역 변경 API 추가 ", "body": "왜"}
        self.assertEqual(publish.commit_message(c, 83), ("Feat: 유저 - 지역 변경 API 추가 (#83)", "왜"))
        self.assertEqual(publish.commit_message({**c, "body": None}, None), ("Feat: 유저 - 지역 변경 API 추가", ""))

    def test_plan_commits_retries_with_reason_then_gives_up(self):
        class Fake:
            def __init__(self, answers):
                self.answers, self.prompts = list(answers), []

            def commit_plan(self, prompt):
                self.prompts.append(prompt)
                return {"commits": self.answers.pop(0)}, 0.1
        good = [{"type": "Feat", "subject": "s", "files": ["a"]}]
        with tempfile.TemporaryDirectory() as tmp:
            repo = make_repo(tmp)
            state = {"goal": "g", "plan": PLAN}
            (repo / "a").write_text("x", encoding="utf-8")
            agents = Fake([[], good])
            commits, cost = publish.plan_commits(agents, repo, state, ["a"])
            self.assertEqual(commits, good)
            self.assertAlmostEqual(cost, 0.2)
            self.assertIn("거부된 이유", agents.prompts[1])
            with self.assertRaises(publish.PublishError):
                publish.plan_commits(Fake([[], []]), repo, state, ["a"])

    def test_publish_makes_one_commit_per_group_and_leaves_other_staged_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = make_repo(tmp)
            sh(repo, "git", "checkout", "-q", "-b", "feature/7-x")
            (repo / "other.txt").write_text("staged earlier", encoding="utf-8")
            sh(repo, "git", "add", "other.txt")
            h = Harness(load_config(), None, repo, repo.parent / (repo.name + "-run"), log=lambda *_: None)
            state = h.new_state("goal")
            state["plan"] = dict(PLAN)
            (repo / "src/main/A.java").write_text("a", encoding="utf-8")
            (repo / "src/main/B.java").write_text("b", encoding="utf-8")
            files = publish.collect_changes(repo, state)
            self.assertEqual(files, ["src/main/A.java", "src/main/B.java"])
            made = publish.make_commits(repo, [
                {"type": "Feat", "subject": "a", "files": ["src/main/A.java"]},
                {"type": "Test", "subject": "b", "body": "why", "files": ["src/main/B.java"]},
            ], 7, log=lambda *_: None)
            self.assertEqual([m["subject"] for m in made], ["Feat: a (#7)", "Test: b (#7)"])
            log = guard.git(repo, "log", "--format=%s", "-3").stdout.splitlines()
            self.assertEqual(log[:2], ["Test: b (#7)", "Feat: a (#7)"])
            self.assertEqual(guard.git(repo, "diff", "--cached", "--name-only").stdout.split(), ["other.txt"])


class ParallelTest(unittest.TestCase):
    def test_worktree_path_is_sibling(self):
        p = parallel.worktree_path("/a/b/kombatzzang", 93)
        self.assertEqual(p.name, "kombatzzang-wt-93")
        self.assertEqual(p.parent.name, "b")

    def test_pick_issues_skips_branched_and_assigned_newest_first(self):
        issues = [{"number": 95, "title": "a", "assignees": []}, {"number": 93, "title": "b", "assignees": []},
                  {"number": 92, "title": "c", "assignees": [{"login": "x"}]}, {"number": 48, "title": "d", "assignees": []}]
        orig = parallel._gh_json
        parallel._gh_json = lambda *a, **k: issues
        try:
            picked = parallel.pick_issues(".", 2, ["feature/95-user-admin"])
        finally:
            parallel._gh_json = orig
        self.assertEqual([i["number"] for i in picked], [93, 48])

    def test_add_worktree_creates_branch_and_reuses(self):
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / "repo").mkdir()
            repo = make_repo(Path(tmp) / "repo")
            sh(repo, "git", "branch", "dev")
            path, branch = parallel.add_worktree(repo, 7, "some-work", "dev")
            self.assertEqual(branch, "feature/7-some-work")
            self.assertTrue(path.is_dir())
            self.assertEqual(parallel.ahead_count(path, "dev"), 0)
            self.assertEqual(parallel.add_worktree(repo, 7, "other", "dev"), (path, branch))
            sh(repo, "git", "worktree", "remove", "--force", str(path))

    def test_open_pr_uses_latest_draft(self):
        with tempfile.TemporaryDirectory() as tmp:
            run = Path(tmp) / "harness/runs/20260101-000000"
            run.mkdir(parents=True)
            (run / "pr-title.txt").write_text("Feat: t (#1)\n", encoding="utf-8")
            (run / "pr-body.md").write_text("body", encoding="utf-8")
            calls = []
            orig_json, orig_gh = parallel._gh_json, branching._gh
            parallel._gh_json = lambda *a, **k: []
            branching._gh = lambda repo, *a: calls.append(a) or "https://github.com/o/r/pull/9\n"
            try:
                url = parallel.open_pr(load_config(), tmp, "feature/1-x", "dev")
            finally:
                parallel._gh_json, branching._gh = orig_json, orig_gh
            self.assertTrue(url.endswith("/pull/9"))
            args = calls[0]
            self.assertEqual(args[args.index("--title") + 1], "Feat: t (#1)")
            self.assertEqual(args[args.index("--head") + 1], "feature/1-x")

    def test_open_pr_reuses_existing_and_requires_draft(self):
        orig = parallel._gh_json
        parallel._gh_json = lambda *a, **k: [{"url": "https://x/pull/3"}]
        try:
            self.assertEqual(parallel.open_pr(load_config(), ".", "b", "dev"), "https://x/pull/3")
            parallel._gh_json = lambda *a, **k: []
            with tempfile.TemporaryDirectory() as tmp:
                with self.assertRaises(parallel.ParallelError):
                    parallel.open_pr(load_config(), tmp, "b", "dev")
        finally:
            parallel._gh_json = orig

    def test_summarize_lists_every_issue(self):
        out = parallel.summarize([{"issue": 1, "status": "PUSHED", "branch": "feature/1-a", "detail": ""},
                                  {"issue": 2, "status": "FAILED", "branch": None, "detail": "x"}])
        self.assertIn("#1", out)
        self.assertIn("FAILED", out)


class BatchTest(unittest.TestCase):
    def test_paths_overlap_same_file_or_parent_dir(self):
        self.assertTrue(parallel.paths_overlap({"a/B.java"}, {"a/B.java"}))
        self.assertTrue(parallel.paths_overlap({"a/"}, {"a/b/C.java"}))
        self.assertTrue(parallel.paths_overlap({"a/b"}, {"a/b/C.java"}))
        self.assertFalse(parallel.paths_overlap({"a/B.java"}, {"a/BC.java"}))
        self.assertFalse(parallel.paths_overlap(set(), {"a/"}))

    def test_split_batches_separates_overlapping_issues(self):
        touch = {1: {"x/A.java"}, 2: {"x/A.java"}, 3: {"y/B.java"}}
        self.assertEqual(parallel.split_batches([1, 2, 3], touch), [[1, 3], [2]])
        self.assertEqual(parallel.split_batches([1, 2], {}), [[1, 2]])


class ResolverFake:
    """충돌 마커를 지우고 양쪽 줄을 모두 남긴다."""
    def __init__(self, path):
        self.path = path

    def resolve(self, prompt, files):
        for f in files:
            text = (self.path / f).read_text(encoding="utf-8")
            kept = [l for l in text.splitlines() if not l.startswith(("<<<<<<<", "=======", ">>>>>>>"))]
            (self.path / f).write_text("\n".join(kept) + "\n", encoding="utf-8")
        return {"summary": "both", "resolved_files": list(files)}, 0.1


class IntegrateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        (root / "repo").mkdir()
        self.repo = make_repo(root / "repo")
        (self.repo / "src/main/App.txt").write_text("base\n", encoding="utf-8")
        sh(self.repo, "git", "add", ".")
        sh(self.repo, "git", "commit", "-qm", "app")
        sh(self.repo, "git", "branch", "dev")
        self.wt = {n: parallel.add_worktree(self.repo, n, "w", "dev")[0] for n in (1, 2)}

    def tearDown(self):
        for p in self.wt.values():
            sh(self.repo, "git", "worktree", "remove", "--force", str(p))
        self.tmp.cleanup()

    def commit(self, n, line):
        (self.wt[n] / "src/main/App.txt").write_text(f"base\n{line}\n", encoding="utf-8")
        sh(self.wt[n], "git", "commit", "-qam", f"issue {n}")

    def integrator(self, ok=True):
        passed = lambda cfg, plan, repo, log: VerifyResult(ok, 0 if ok else 1, False, "", [])
        return integrate.Integrator(load_config(), "dev", lambda path: ResolverFake(path), passed, log=lambda *a: None)

    def test_rebase_conflict_with_base_is_resolved(self):
        self.commit(1, "from-issue")
        (self.repo / "src/main/App.txt").write_text("base\nfrom-dev\n", encoding="utf-8")
        sh(self.repo, "git", "commit", "-qam", "dev moved")
        sh(self.repo, "git", "branch", "-f", "dev")
        info = self.integrator().integrate(1, self.wt[1], "feature/1-w", "t")
        self.assertTrue(info["changed"])
        text = (self.wt[1] / "src/main/App.txt").read_text(encoding="utf-8")
        self.assertIn("from-dev", text)
        self.assertIn("from-issue", text)
        self.assertEqual(guard.git(self.wt[1], "status", "--porcelain").stdout, "")

    def test_conflicting_sibling_is_merged_in(self):
        self.commit(1, "one")
        self.commit(2, "two")
        integ = self.integrator()
        first = integ.integrate(1, self.wt[1], "feature/1-w", "t")
        self.assertFalse(first["changed"])
        second = integ.integrate(2, self.wt[2], "feature/2-w", "t")
        self.assertEqual(second["merged"], ["feature/1-w"])
        text = (self.wt[2] / "src/main/App.txt").read_text(encoding="utf-8")
        self.assertIn("one", text)
        self.assertIn("two", text)

    def test_failed_verify_after_change_raises(self):
        self.commit(1, "one")
        (self.repo / "README.md").write_text("moved", encoding="utf-8")
        sh(self.repo, "git", "commit", "-qam", "dev moved")
        sh(self.repo, "git", "branch", "-f", "dev")
        with self.assertRaises(integrate.IntegrationError):
            self.integrator(ok=False).integrate(1, self.wt[1], "feature/1-w", "t")


class VerifyFrontendTest(unittest.TestCase):
    """verify.run_verify 의 프론트엔드 단계. 실제 gradle/npm 대신 verify._run 을 가짜로 바꿔 검증한다."""

    def setUp(self):
        import verify
        self.verify = verify
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        (self.repo / "frontend" / "node_modules").mkdir(parents=True)
        self.write_pkg({"test": "vitest"})
        self.cfg = load_config()
        self.calls = []
        self.codes = {}  # 명령 종류별 종료 코드. 기본은 모두 0
        self._orig = (verify._run, verify.frontend_changed, verify.shutil.which)
        verify._run = self.fake_run
        verify.frontend_changed = lambda cfg, repo: self.changed
        verify.shutil.which = lambda name: "npm"
        self.changed = True

    def tearDown(self):
        self.verify._run, self.verify.frontend_changed, self.verify.shutil.which = self._orig
        self.tmp.cleanup()

    def write_pkg(self, scripts):
        (self.repo / "frontend" / "package.json").write_text(json.dumps({"scripts": scripts}), encoding="utf-8")
        os.utime(self.repo / "frontend" / "node_modules", (9e9, 9e9))  # node_modules 가 더 최근 → 설치 불필요

    def fake_run(self, cmd, cwd, timeout, env=None):
        kind = "backend" if "gradlew" in cmd[0] else ("install" if "install" in cmd else "test")
        self.calls.append(kind)
        return self.codes.get(kind, 0), f"{kind} output", False

    def run_verify(self):
        return self.verify.run_verify(self.cfg, {}, str(self.repo), self.repo / "verify.log")

    def test_skips_frontend_when_unchanged(self):
        self.changed = False
        r = self.run_verify()
        self.assertTrue(r.passed)
        self.assertEqual(self.calls, ["backend"])

    def test_runs_frontend_tests_when_changed(self):
        r = self.run_verify()
        self.assertTrue(r.passed)
        self.assertEqual(self.calls, ["backend", "test"])  # node_modules 가 최신이라 설치는 생략
        self.assertEqual(r.stages, [("backend", True), ("frontend", True)])

    def test_frontend_failure_fails_even_if_backend_passes(self):
        self.codes["test"] = 1
        r = self.run_verify()
        self.assertFalse(r.passed)
        self.assertEqual(r.exit_code, 1)
        self.assertIn("[frontend] FAIL", r.log_tail)

    def test_node_options_passed_to_npm_env(self):
        envs = []
        orig = self.verify._run
        self.verify._run = lambda cmd, cwd, timeout, env=None: (envs.append(env), orig(cmd, cwd, timeout, env))[1]
        self.cfg["verify"]["frontend"]["node_options"] = "--no-experimental-webstorage"
        self.run_verify()
        self.assertIn("--no-experimental-webstorage", envs[-1]["NODE_OPTIONS"])
        self.assertEqual(envs[-1]["CI"], "1")

    def test_missing_test_script_fails(self):
        self.write_pkg({"dev": "vite"})
        r = self.run_verify()
        self.assertFalse(r.passed)
        self.assertNotIn("test", self.calls)

    def test_install_failure_skips_tests_and_retries_once(self):
        os.utime(self.repo / "frontend" / "node_modules", (1, 1))  # package.json 이 더 최근 → 설치 필요
        self.codes["install"] = 1
        r = self.run_verify()
        self.assertFalse(r.passed)
        self.assertEqual(self.calls, ["backend", "install", "install"])


class SchemaTest(unittest.TestCase):
    def test_validate(self):
        validate(PLAN, PLAN_SCHEMA)
        with self.assertRaises(SchemaError):
            validate({"summary": "s"}, PLAN_SCHEMA)


if __name__ == "__main__":
    unittest.main()
