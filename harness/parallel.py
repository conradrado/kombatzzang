"""이슈 여러 개를 git worktree 로 나눠 병렬로 구현하고, 성공한 브랜치를 push 한다.

사용법:
    python harness/parallel.py 93 92            # 지정한 이슈 병렬 실행
    python harness/parallel.py --auto 2         # 브랜치/담당자 없는 열린 이슈 중 최신순 2개 자동 선택
    python harness/parallel.py 93 92 --no-push  # 커밋까지만
    python harness/parallel.py 93 92 --no-pr    # push 까지만
    python harness/parallel.py 93 --cleanup     # push 성공한 worktree 는 삭제

이슈마다 `<저장소>-wt-<번호>` worktree 와 `feature/<번호>-<slug>` 브랜치를 만들고, 그 안에서 `run.py` 를 실행한다.
push 는 에이전트가 아니라 이 오케스트레이터가 한다. 테스트 green + Evaluator PASS + 커밋 완료(run.py 종료 코드 0)인 브랜치만 push 한다.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import branching
import guard
from config import REPO_ROOT, RUNS_DIR, load_config


class ParallelError(RuntimeError):
    pass


def worktree_path(repo, number):
    """저장소 옆 형제 폴더. 기존 `kombatzzang-wt-73` 관례를 따른다."""
    repo = Path(repo).resolve()
    return repo.parent / f"{repo.name}-wt-{number}"


def _gh_json(repo, *args):
    return json.loads(branching._gh(repo, *args))


def pick_issues(repo, count, branches):
    """브랜치도 담당자도 없는 열린 이슈를 번호 큰(최신) 순으로 count 개 고른다."""
    taken = {int(m.group(1)) for b in branches if (m := re.match(r"feature/(\d+)-", b))}
    issues = _gh_json(repo, "issue", "list", "--state", "open", "--limit", "100", "--json", "number,title,assignees")
    free = [i for i in issues if i["number"] not in taken and not i["assignees"]]
    return sorted(free, key=lambda i: i["number"], reverse=True)[:count]


def make_slug(title):
    """영문 kebab-case 슬러그. 한글 제목이라 CLI 로 짧게 요청하고, 실패하면 'work'."""
    prompt = ("다음 이슈 제목을 브랜치 이름용 영문 소문자 kebab-case 로 2~4단어로 줄여 출력해라. "
              f"도메인-작업 순서(예: battle-sse-notification). 슬러그 외에는 아무것도 출력하지 마라.\n\n{title}")
    try:
        proc = subprocess.run(["claude", "-p", "--model", "haiku", "--no-session-persistence",
                               "--disable-slash-commands", "--strict-mcp-config", "--tools", ""],
                              input=prompt, capture_output=True, text=True, encoding="utf-8",
                              errors="replace", timeout=120, shell=(os.name == "nt"))
        return branching._slugify(proc.stdout.strip().splitlines()[-1] if proc.stdout.strip() else "")
    except (OSError, subprocess.TimeoutExpired):
        return "work"


def add_worktree(repo, number, slug, base):
    """worktree 와 브랜치를 만든다. 이미 만든 worktree 가 있으면 재사용한다. (경로, 브랜치) 반환."""
    path = worktree_path(repo, number)
    existing = [b for b in branching.list_branches(repo) if b.startswith(f"feature/{number}-")]
    if path.exists():
        return path, guard.current_branch(path)
    if existing:
        branch = existing[0]
        local = guard.git(repo, "rev-parse", "--verify", "--quiet", f"refs/heads/{branch}", check=False).returncode == 0
        args = ["worktree", "add", str(path), branch] if local else \
            ["worktree", "add", "--track", "-b", branch, str(path), f"origin/{branch}"]
    else:
        branch = f"feature/{number}-{slug}"
        args = ["worktree", "add", "--no-track", "-b", branch, str(path), branching._base_ref(repo, base)]
    try:
        guard.git(repo, *args)
    except RuntimeError as e:  # 같은 브랜치가 다른 worktree 에서 체크아웃 중인 경우 등
        raise ParallelError(str(e)) from e
    return path, branch


def ahead_count(path, base_ref):
    return int(guard.git(path, "rev-list", "--count", f"{base_ref}..HEAD").stdout.strip() or 0)


def latest_draft(path):
    """worktree 안 가장 최근 run 의 PR 초안 (제목 파일, 본문 파일). 없으면 None."""
    titles = sorted(Path(path).glob("harness/runs/*/pr-title.txt"), key=lambda f: f.stat().st_mtime)
    if titles and (titles[-1].parent / "pr-body.md").is_file():
        return titles[-1], titles[-1].parent / "pr-body.md"
    return None


def open_pr(cfg, path, branch, base):
    """PR 초안으로 PR 을 등록하고 URL 을 반환한다. 이미 열린 PR 이 있으면 그 URL 을 반환한다."""
    existing = _gh_json(path, "pr", "list", "--head", branch, "--state", "open", "--json", "url")
    if existing:
        return existing[0]["url"]
    draft = latest_draft(path)
    if not draft:
        raise ParallelError("PR 초안(pr-title.txt, pr-body.md)을 찾을 수 없다")
    title, body = draft
    args = ["pr", "create", "--base", base, "--head", branch,
            "--title", title.read_text(encoding="utf-8").strip(), "--body-file", str(body)]
    assignee = os.environ.get(cfg["git"].get("assignee_env", ""))
    if assignee:
        args += ["--assignee", assignee]
    return branching._gh(path, *args).strip().splitlines()[-1]


def run_issue(cfg, issue, args, log_dir, base_ref):
    """이슈 하나를 처리하고 결과 dict 를 돌려준다. 예외는 결과에 담아 다른 이슈에 영향을 주지 않는다."""
    number = issue["number"]
    result = {"issue": number, "title": issue["title"], "status": "FAILED", "detail": "", "branch": None, "path": None}
    try:
        slug = args.slug.get(number) or make_slug(issue["title"])
        path, branch = add_worktree(REPO_ROOT, number, slug, cfg["git"].get("base_branch", "dev"))
        result.update(branch=branch, path=str(path))
        info = _gh_json(REPO_ROOT, "issue", "view", str(number), "--json", "title,body")
        goal = path / "harness" / "runs" / f"goal-issue-{number}.md"
        goal.parent.mkdir(parents=True, exist_ok=True)
        goal.write_text(f"이슈 #{number}: {info['title']}\n\n{info['body'] or ''}\n", encoding="utf-8")

        log_file = log_dir / f"issue-{number}.log"
        cmd = [sys.executable, str(path / "harness" / "run.py"), "--goal-file", str(goal), "--no-branch"]
        if args.max_iterations:
            cmd += ["--max-iterations", str(args.max_iterations)]
        with open(log_file, "w", encoding="utf-8") as f:
            code = subprocess.run(cmd, cwd=path, stdout=f, stderr=subprocess.STDOUT,
                                  env={**os.environ, "PYTHONUTF8": "1"}).returncode
        result["log"] = str(log_file)
        if code != 0:
            result.update(status="ABORTED" if code == 2 else "FAILED", detail=f"run.py 종료 코드 {code} (로그: {log_file})")
            return result
        if ahead_count(path, base_ref) == 0:
            result.update(status="NO_COMMITS", detail="성공했지만 만들어진 커밋이 없다")
            return result
        if args.no_push:
            result.update(status="COMMITTED", detail="--no-push")
            return result
        guard.git(path, "push", "-u", "origin", branch)
        result.update(status="PUSHED")
        if not args.no_pr:
            result.update(status="PR_CREATED", detail=open_pr(cfg, path, branch, cfg["git"].get("base_branch", "dev")))
    except (ParallelError, branching.BranchError, RuntimeError) as e:
        result["detail"] = str(e)  # push 성공 후 PR 등록만 실패하면 status 는 PUSHED 로 남는다
    return result


def summarize(results):
    lines = [f"{'이슈':<6}{'상태':<11}{'브랜치':<45}비고"]
    lines += [f"#{r['issue']:<5}{r['status']:<11}{(r['branch'] or '-'):<45}{r['detail']}" for r in results]
    return "\n".join(lines)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    ap = argparse.ArgumentParser(description="worktree 병렬 이슈 구현 + push")
    ap.add_argument("issues", nargs="*", type=int, help="처리할 이슈 번호")
    ap.add_argument("--auto", type=int, metavar="N", help="이슈 번호가 없을 때 자동으로 고를 개수")
    ap.add_argument("--workers", type=int, default=3, help="동시에 돌릴 최대 개수 (기본 3)")
    ap.add_argument("--slug", action="append", default=[], metavar="번호=slug", help="브랜치 슬러그 직접 지정")
    ap.add_argument("--max-iterations", type=int)
    ap.add_argument("--no-push", action="store_true", help="커밋까지만 하고 push 하지 않음")
    ap.add_argument("--no-pr", action="store_true", help="push 까지만 하고 PR 은 등록하지 않음")
    ap.add_argument("--cleanup", action="store_true", help="push 성공한 worktree 를 삭제")
    ap.add_argument("--yes", action="store_true", help="자동 선택 확인 생략")
    args = ap.parse_args()
    args.slug = {int(k): v for k, v in (s.split("=", 1) for s in args.slug)}
    if not args.issues and not args.auto:
        ap.error("이슈 번호 또는 --auto N 이 필요합니다.")

    cfg = load_config()
    base = cfg["git"].get("base_branch", "dev")
    guard.git(REPO_ROOT, "fetch", "origin", check=False)
    if args.issues:
        by_number = {i["number"]: i for i in branching.list_issues(REPO_ROOT)}
        missing = [n for n in args.issues if n not in by_number]
        if missing:
            sys.exit(f"열린 이슈가 아님: {missing}")
        issues = [by_number[n] for n in args.issues]
    else:
        issues = pick_issues(REPO_ROOT, args.auto, branching.list_branches(REPO_ROOT))
        if not issues:
            sys.exit("자동으로 고를 이슈가 없다 (브랜치/담당자가 없는 열린 이슈)")
        print("자동 선택:\n" + "\n".join(f"  #{i['number']} {i['title']}" for i in issues))
        if not branching._confirm("이 이슈들을 진행할까요?", args.yes):
            sys.exit("취소됨")

    cost_cap = cfg["limits"]["max_total_cost_usd"] * len(issues)
    log_dir = RUNS_DIR / f"parallel-{datetime.now():%Y%m%d-%H%M%S}"
    log_dir.mkdir(parents=True)
    base_ref = branching._base_ref(REPO_ROOT, base)
    workers = max(1, min(args.workers, len(issues)))
    print(f"이슈 {len(issues)}개, 동시 {workers}개, 비용 상한 최대 ${cost_cap:.2f}, 로그: {log_dir}")

    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(lambda i: run_issue(cfg, i, args, log_dir, base_ref), issues))

    for r in results:  # PR 초안은 worktree 가 지워져도 남도록 로그 폴더로 모은다
        if r["path"]:
            for f in Path(r["path"]).glob("harness/runs/*/pr-*"):
                shutil.copy(f, log_dir / f"issue-{r['issue']}-{f.name}")
    if args.cleanup:
        for r in results:
            if r["status"] in ("PUSHED", "PR_CREATED"):
                guard.git(REPO_ROOT, "worktree", "remove", "--force", r["path"], check=False)
                shutil.rmtree(r["path"], ignore_errors=True)
    print("\n" + summarize(results))
    for r in results:
        draft = f"{log_dir}/issue-{r['issue']}-pr-body.md"
        if r["status"] == "PUSHED":
            print(f"PR 등록 필요: gh pr create --base {base} --head {r['branch']}  (초안: {draft})")
        elif r["status"] == "COMMITTED":
            print(f"push: git -C {r['path']} push -u origin {r['branch']}  (PR 초안: {draft})")
    sys.exit(0 if all(r["status"] in ("PR_CREATED", "PUSHED", "COMMITTED") for r in results) else 1)


if __name__ == "__main__":
    main()
