"""가짜 에이전트 + 임시 git 저장소로 루프/가드 동작을 검증한다. (claude CLI, gradle 불필요)

실행: python harness/tests/test_harness.py
"""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import guard
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

    def generate(self, prompt):
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


class SchemaTest(unittest.TestCase):
    def test_validate(self):
        validate(PLAN, PLAN_SCHEMA)
        with self.assertRaises(SchemaError):
            validate({"summary": "s"}, PLAN_SCHEMA)


if __name__ == "__main__":
    unittest.main()
