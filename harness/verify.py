"""하네스가 직접 테스트를 실행한다. 에이전트의 '통과했다'는 말이 아니라 실제 종료 코드로 판정."""
import os
import re
import subprocess
from dataclasses import dataclass


@dataclass
class VerifyResult:
    passed: bool
    exit_code: int
    timed_out: bool
    log_tail: str
    failure_lines: list


FAILURE_PATTERN = re.compile(r"FAILED|error:|Exception|AssertionError|BUILD FAILED", re.I)


def build_command(cfg, plan, repo):
    base = list(cfg["verify"]["command"])
    wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"
    base[0] = os.path.join(repo, wrapper)
    for f in plan.get("test_filter") or []:
        base += ["--tests", f]
    return base


def run_verify(cfg, plan, repo, log_path):
    cmd = build_command(cfg, plan, repo)
    timeout = cfg["limits"]["verify_timeout_sec"]
    timed_out = False
    try:
        proc = subprocess.run(
            cmd, cwd=repo, capture_output=True, text=True,
            encoding="utf-8", errors="replace", timeout=timeout,
        )
        output, code = proc.stdout + proc.stderr, proc.returncode
    except subprocess.TimeoutExpired as e:
        timed_out, code = True, -1
        output = f"(시간 초과 {timeout}s)\n" + _to_text(e.stdout) + _to_text(e.stderr)
    with open(log_path, "w", encoding="utf-8") as f:
        f.write(f"$ {' '.join(cmd)}\n\n{output}")
    tail = output[-cfg["verify"]["log_tail_chars"]:]
    failures = [l.strip() for l in output.splitlines() if FAILURE_PATTERN.search(l)]
    return VerifyResult(code == 0 and not timed_out, code, timed_out, tail, failures[:30])


def _to_text(x):
    if x is None:
        return ""
    return x.decode("utf-8", "replace") if isinstance(x, bytes) else x
