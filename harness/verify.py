"""하네스가 직접 테스트를 실행한다. 에이전트의 '통과했다'는 말이 아니라 실제 종료 코드로 판정."""
import os
import re
import subprocess
from dataclasses import dataclass


@dataclass
class VerifyResult:
    """테스트 실행 결과."""
    passed: bool         # 종료 코드 0 이고 시간 초과가 아니면 True
    exit_code: int       # 프로세스 종료 코드 (시간 초과 시 -1)
    timed_out: bool      # verify_timeout_sec 초과 여부
    log_tail: str        # 로그 끝부분 (Evaluator/Generator 프롬프트에 넣는 용도)
    failure_lines: list  # 실패 패턴에 걸린 줄들 (정체 판정용 signature 계산에 사용)


# 실패로 볼 만한 줄을 골라내는 패턴 (대소문자 무시)
FAILURE_PATTERN = re.compile(r"FAILED|error:|Exception|AssertionError|BUILD FAILED", re.I)


def build_command(cfg, plan, repo):
    """config.toml 의 명령을 바탕으로 실제 실행할 gradle 명령 리스트를 만든다."""
    base = list(cfg["verify"]["command"])
    # 첫 항목(gradlew)을 OS에 맞는 래퍼(Windows: gradlew.bat)의 저장소 내 절대 경로로 교체
    wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"
    base[0] = os.path.join(repo, wrapper)
    # Planner 가 지정한 테스트 필터가 있으면 해당 테스트만 실행하도록 --tests 옵션 추가
    for f in plan.get("test_filter") or []:
        base += ["--tests", f]
    return base


def run_verify(cfg, plan, repo, log_path):
    """테스트를 실행하고 전체 로그를 log_path 에 저장한 뒤 VerifyResult 를 반환한다."""
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
        # 시간 초과 시 그때까지 출력된 내용만이라도 로그에 남긴다
        timed_out, code = True, -1
        output = f"(시간 초과 {timeout}s)\n" + _to_text(e.stdout) + _to_text(e.stderr)
    # 실행한 명령어를 첫 줄에 적어 전체 로그를 파일로 저장
    with open(log_path, "w", encoding="utf-8") as f:
        f.write(f"$ {' '.join(cmd)}\n\n{output}")
    # 프롬프트가 너무 길어지지 않도록 로그의 끝부분만 잘라 전달 (실패 원인은 보통 끝에 나온다)
    tail = output[-cfg["verify"]["log_tail_chars"]:]
    # 실패 패턴이 있는 줄만 추려 최대 30줄까지 보관
    failures = [l.strip() for l in output.splitlines() if FAILURE_PATTERN.search(l)]
    return VerifyResult(code == 0 and not timed_out, code, timed_out, tail, failures[:30])


def _to_text(x):
    """TimeoutExpired 의 stdout/stderr 는 None 또는 bytes 일 수 있어 문자열로 통일한다."""
    if x is None:
        return ""
    return x.decode("utf-8", "replace") if isinstance(x, bytes) else x
