"""변경 파일 범위 검사. plan 의 allowed_paths 밖을 건드린 변경은 되돌린다."""
import os
import re
import subprocess

# plan 과 무관하게 에이전트가 수정할 수 없는 경로
ALWAYS_FORBIDDEN = (".git/", ".github/", ".claude/", "harness/")


def git(repo, *args, check=True):
    proc = subprocess.run(
        ["git", *args], cwd=repo, capture_output=True, text=True,
        encoding="utf-8", errors="replace",
    )
    if check and proc.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} 실패: {proc.stderr.strip()}")
    return proc


def current_branch(repo):
    return git(repo, "branch", "--show-current").stdout.strip()


def _file_hash(repo, path):
    full = os.path.join(repo, path)
    if not os.path.isfile(full):
        return "DELETED"
    return git(repo, "hash-object", "--", path).stdout.strip()


def snapshot(repo):
    """작업 트리에서 변경된 경로 -> 내용 해시. 하네스 시작 시점의 기준선으로도 쓴다."""
    out = git(repo, "status", "--porcelain", "-uall", "-z").stdout
    entries = [e for e in out.split("\0") if e]
    paths, i = [], 0
    while i < len(entries):
        status, path = entries[i][:2], entries[i][3:]
        paths.append(path)
        i += 2 if status[0] in "RC" else 1  # rename/copy 는 원본 경로 항목이 하나 더 있음
    return {p: _file_hash(repo, p) for p in paths}


def changed_since(baseline, current):
    changed = [p for p, h in current.items() if baseline.get(p) != h]
    changed += [p for p in baseline if p not in current]  # 기준선에서 더러웠다가 깨끗해진 파일
    return sorted(set(changed))


def _glob_to_regex(pattern):
    if pattern.endswith("/"):
        pattern += "**"
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**", i):
            out += ".*"
            i += 2
        elif pattern[i] == "*":
            out += "[^/]*"
            i += 1
        elif pattern[i] == "?":
            out += "[^/]"
            i += 1
        else:
            out += re.escape(pattern[i])
            i += 1
    return re.compile(out + r"\Z")


def in_scope(path, allowed_paths):
    path = path.replace("\\", "/")
    if path.startswith(ALWAYS_FORBIDDEN):
        return False
    return any(_glob_to_regex(p.replace("\\", "/")).match(path) for p in allowed_paths)


def split_violations(changed, allowed_paths):
    ok = [p for p in changed if in_scope(p, allowed_paths)]
    bad = [p for p in changed if not in_scope(p, allowed_paths)]
    return ok, bad


def revert(repo, violations, baseline):
    """범위를 벗어난 변경을 되돌린다. 기준선부터 더러웠던 파일은 복원할 수 없어 unreverted 로 보고한다."""
    reverted, unreverted = [], []
    for p in violations:
        if p in baseline:
            unreverted.append(p)
            continue
        tracked = git(repo, "ls-files", "--error-unmatch", "--", p, check=False).returncode == 0
        if tracked:
            git(repo, "checkout", "HEAD", "--", p)
        elif os.path.isfile(os.path.join(repo, p)):
            os.remove(os.path.join(repo, p))
        reverted.append(p)
    return reverted, unreverted


def build_diff(repo, paths, limit=200_000):
    """Evaluator 에게 보여줄 diff. 추적 중인 파일은 git diff, 신규 파일은 내용 전체."""
    if not paths:
        return ""
    tracked = set(git(repo, "ls-files", "--", *paths).stdout.splitlines())
    parts = []
    tracked_paths = [p for p in paths if p in tracked]
    if tracked_paths:
        parts.append(git(repo, "diff", "HEAD", "--", *tracked_paths).stdout)
    for p in paths:
        if p in tracked:
            continue
        full = os.path.join(repo, p)
        if os.path.isfile(full):
            with open(full, encoding="utf-8", errors="replace") as f:
                parts.append(f"=== NEW FILE: {p} ===\n{f.read()}\n")
    diff = "\n".join(parts)
    if len(diff) > limit:
        diff = diff[:limit] + f"\n... (diff 가 {limit}자를 넘어 잘림)"
    return diff
