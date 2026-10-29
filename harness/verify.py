"""하네스가 직접 테스트를 실행한다. 에이전트의 '통과했다'는 말이 아니라 실제 종료 코드로 판정."""
import json
import os
import re
import shutil
import subprocess
from dataclasses import dataclass, field
from pathlib import Path

import guard


@dataclass
class VerifyResult:
    """테스트 실행 결과."""
    passed: bool         # 모든 단계가 종료 코드 0 이고 시간 초과가 없으면 True
    exit_code: int       # 처음 실패한 단계의 종료 코드 (모두 통과하면 0, 시간 초과 시 -1)
    timed_out: bool      # 시간 초과 여부
    log_tail: str        # 로그 끝부분 (Evaluator/Generator 프롬프트에 넣는 용도)
    failure_lines: list  # 실패 패턴에 걸린 줄들 (정체 판정용 signature 계산에 사용)
    stages: list = field(default_factory=list)  # [(단계 이름, 통과 여부)] 실제로 실행한 단계들


# 실패로 볼 만한 줄을 골라내는 패턴 (대소문자 무시)
FAILURE_PATTERN = re.compile(r"FAILED|error:|Exception|AssertionError|BUILD FAILED|^\s*FAIL\s|ERR!", re.I)


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


def frontend_changed(cfg, repo):
    """이 worktree 에서 프론트엔드 디렉터리가 바뀌었는지 본다. 커밋 전(status)과 커밋 후(base 대비 diff)를 모두 확인한다."""
    d = cfg["verify"]["frontend"]["dir"]
    if guard.git(repo, "status", "--porcelain", "--", d, check=False).stdout.strip():
        return True
    base = cfg["git"]["base_branch"]
    # 로컬에 base 브랜치가 없으면 origin/<base> 로 한 번 더 시도한다
    for ref in (base, f"origin/{base}"):
        proc = guard.git(repo, "diff", "--name-only", f"{ref}...HEAD", "--", d, check=False)
        if proc.returncode == 0:
            return bool(proc.stdout.strip())
    return False


def run_verify(cfg, plan, repo, log_path):
    """단계별로 테스트를 실행하고 전체 로그를 log_path 에 저장한 뒤 VerifyResult 를 반환한다."""
    timeout = cfg["limits"]["verify_timeout_sec"]
    sections = []   # 로그 파일에 순서대로 이어 쓸 단계별 출력
    stages = []     # (단계 이름, 통과 여부)
    first_code, timed_out = 0, False

    def record(name, code, out, to=False):
        nonlocal first_code, timed_out
        ok = code == 0 and not to
        stages.append((name, ok))
        sections.append(f"##### [{name}] {'PASS' if ok else 'FAIL'} (exit={code})\n{out}")
        if not ok and first_code == 0:
            first_code = code
        timed_out = timed_out or to
        return ok

    # 1) 백엔드: 프론트만 바꿔도 항상 돌린다 (UP-TO-DATE 여도 무해)
    cmd = build_command(cfg, plan, repo)
    code, out, to = _run(cmd, repo, timeout)
    record("backend", code, f"$ {' '.join(cmd)}\n\n{out}", to)

    # 2) 프론트엔드: frontend/ 가 바뀐 경우에만. 앞 단계가 실패해도 원인 파악을 위해 끝까지 돌린다.
    fe = cfg["verify"].get("frontend") or {}
    if fe.get("enabled") and frontend_changed(cfg, repo):
        _verify_frontend(fe, repo, record)

    # 단계별 요약을 맨 위에 붙여, tail 로 잘려도 어느 단계가 실패했는지 보이게 한다
    summary = " / ".join(f"[{n}] {'PASS' if ok else 'FAIL'}" for n, ok in stages)
    output = "\n\n".join(sections)
    with open(log_path, "w", encoding="utf-8") as f:
        f.write(f"{summary}\n\n{output}")
    # 프롬프트가 너무 길어지지 않도록 로그의 끝부분만 잘라 전달 (실패 원인은 보통 끝에 나온다)
    tail = summary + "\n" + output[-cfg["verify"]["log_tail_chars"]:]
    # 실패 패턴이 있는 줄만 추려 최대 30줄까지 보관
    failures = [l.strip() for l in output.splitlines() if FAILURE_PATTERN.search(l)]
    passed = all(ok for _, ok in stages)
    return VerifyResult(passed, -1 if timed_out else first_code, timed_out, tail, failures[:30], stages)


def _verify_frontend(fe, repo, record):
    """프론트엔드 단계: test 스크립트 확인 → (필요하면) 설치 → 테스트 실행."""
    fe_dir = Path(repo) / fe["dir"]
    npm = shutil.which("npm")  # Windows 에서는 npm.cmd 로 해석된다
    if npm is None:
        record("frontend", 1, "npm 을 찾을 수 없다. Node.js 설치와 PATH 를 확인할 것.")
        return
    # test 스크립트가 없으면 AC 를 검증할 수 없으므로 건너뛰지 않고 실패로 처리해 Generator 에게 알린다
    try:
        scripts = json.loads((fe_dir / "package.json").read_text(encoding="utf-8")).get("scripts", {})
    except (OSError, ValueError) as e:
        record("frontend", 1, f"{fe['dir']}/package.json 을 읽을 수 없다: {e}")
        return
    if "test" not in scripts:
        record("frontend", 1, f"{fe['dir']}/package.json 에 scripts.test 가 없어 프론트 테스트를 실행할 수 없다.")
        return

    env = dict(os.environ, CI="1")  # CI 모드: vitest watch 방지
    if fe.get("node_options"):
        # 기존 NODE_OPTIONS 는 유지하고 뒤에 덧붙인다
        env["NODE_OPTIONS"] = " ".join(filter(None, [env.get("NODE_OPTIONS"), fe["node_options"]]))
    if fe.get("npm_cache"):
        env["npm_config_cache"] = fe["npm_cache"]  # 병렬 worktree 가 설치 캐시를 공유하도록

    if _needs_install(fe_dir):
        install = [npm, *fe["install"][1:]]
        code, out, to = _run(install, fe_dir, fe["install_timeout_sec"], env)
        if code != 0 and not to:   # 캐시 락 같은 일시 오류를 대비해 한 번만 재시도
            code, out, to = _run(install, fe_dir, fe["install_timeout_sec"], env)
        if not record("frontend-install", code, f"$ {' '.join(install)}\n\n{out}", to):
            return  # 설치 실패 시 테스트는 의미가 없다

    test = [npm, *fe["test"][1:]]
    code, out, to = _run(test, fe_dir, fe["test_timeout_sec"], env)
    record("frontend", code, f"$ {' '.join(test)}\n\n{out}", to)


def _needs_install(fe_dir):
    """node_modules 가 없거나 package.json 이 더 최근이면 설치가 필요하다."""
    nm, pkg = fe_dir / "node_modules", fe_dir / "package.json"
    return not nm.is_dir() or pkg.stat().st_mtime > nm.stat().st_mtime


def _run(cmd, cwd, timeout, env=None):
    """명령을 실행해 (종료 코드, 출력, 시간 초과 여부) 를 돌려준다."""
    try:
        proc = subprocess.run(
            cmd, cwd=cwd, capture_output=True, text=True,
            encoding="utf-8", errors="replace", timeout=timeout, env=env,
        )
        return proc.returncode, proc.stdout + proc.stderr, False
    except subprocess.TimeoutExpired as e:
        # 시간 초과 시 그때까지 출력된 내용만이라도 로그에 남긴다
        return -1, f"(시간 초과 {timeout}s)\n" + _to_text(e.stdout) + _to_text(e.stderr), True


def _to_text(x):
    """TimeoutExpired 의 stdout/stderr 는 None 또는 bytes 일 수 있어 문자열로 통일한다."""
    if x is None:
        return ""
    return x.decode("utf-8", "replace") if isinstance(x, bytes) else x
