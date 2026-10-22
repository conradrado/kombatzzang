"""목표와 대응하는 이슈/브랜치를 찾아 작업 브랜치로 이동한다. 없으면 이슈(확인 후)와 브랜치를 만든다."""
import json
import os
import re
import subprocess
import sys

import guard

PROTECTED_NAMES = {"main", "dev", "HEAD"}


class BranchError(RuntimeError):
    pass


def _gh(repo, *args):
    proc = subprocess.run(["gh", *args], cwd=repo, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")
    if proc.returncode != 0:
        raise BranchError(f"gh {' '.join(args[:2])} 실패: {proc.stderr.strip()}")
    return proc.stdout


def list_issues(repo):
    """열린 이슈 [{number, title}]."""
    out = _gh(repo, "issue", "list", "--state", "open", "--limit", "100", "--json", "number,title")
    return json.loads(out)


def list_branches(repo):
    """로컬 + origin 브랜치 이름(중복 제거, origin/ 접두사 제거, main/dev 제외)."""
    out = guard.git(repo, "for-each-ref", "--format=%(refname)", "refs/heads", "refs/remotes/origin").stdout
    names = set()
    for ref in out.splitlines():
        name = re.sub(r"^refs/(heads|remotes/origin)/", "", ref)
        if name not in PROTECTED_NAMES:
            names.add(name)
    return sorted(names)


def route_prompt(goal, issues, branches):
    lines = [f"## 목표\n{goal}\n", "## 열린 이슈 후보"]
    lines += [f"- #{i['number']} {i['title']}" for i in issues] or ["(없음)"]
    lines += ["", "## 브랜치 후보"]
    lines += [f"- {b}" for b in branches] or ["(없음)"]
    return "\n".join(lines) + "\n"


def _slugify(text):
    slug = re.sub(r"[^a-z0-9]+", "-", (text or "").lower()).strip("-")
    return slug or "work"


def decide(route, issues, branches):
    """Router 응답을 검증해 실행할 동작을 정한다. 후보에 없는 값은 거부한다.

    반환: ("checkout", branch) | ("create_branch", issue_number, branch) | ("create_issue", route)
    """
    match = route["match"]
    if match == "branch":
        if route.get("branch") not in branches:
            raise BranchError(f"Router 가 후보에 없는 브랜치를 골랐다: {route.get('branch')!r}")
        return ("checkout", route["branch"])
    if match == "issue":
        number = route.get("issue_number")
        if number not in {i["number"] for i in issues}:
            raise BranchError(f"Router 가 후보에 없는 이슈를 골랐다: {number!r}")
        # 같은 이슈 번호로 이미 만든 브랜치가 있으면 그것을 쓴다.
        existing = [b for b in branches if b.startswith(f"feature/{number}-")]
        if existing:
            return ("checkout", existing[0])
        return ("create_branch", number, f"feature/{number}-{_slugify(route.get('slug'))}")
    if not route.get("title") or not route.get("tasks"):
        raise BranchError("새 이슈를 만들려면 Router 응답에 title 과 tasks 가 필요하다")
    return ("create_issue", route)


def issue_body(route):
    """.github/ISSUE_TEMPLATE/issue-template.md 의 섹션 구조를 따른 본문."""
    tasks = "\n".join(f"- [ ] {t}" for t in route["tasks"])
    return (f"# 📌 Issue\n\n## 📖 Description\n\n{route.get('description') or route['title']}\n\n---\n\n"
            f"## 🎯 Goal\n\n{route.get('goal') or route.get('description') or route['title']}\n\n---\n\n"
            f"## ✅ Tasks\n\n{tasks}\n\n---\n\n## 📎 References (Optional)\n\n없음\n")


def _confirm(message, assume_yes):
    if assume_yes:
        return True
    if not sys.stdin.isatty():
        return False
    return input(f"{message} [y/N] ").strip().lower() in ("y", "yes")


def _create_issue(repo, cfg, route, body_path, assume_yes, log):
    log(f"새 이슈 생성 제안:\n  제목: {route['title']}\n" + "\n".join(f"  - {t}" for t in route["tasks"]))
    if not _confirm("GitHub 에 이슈를 등록할까요?", assume_yes):
        raise BranchError("이슈 등록이 승인되지 않았다 (--yes 로 자동 승인하거나 --no-branch 로 건너뛴다)")
    body_path.parent.mkdir(parents=True, exist_ok=True)
    body_path.write_text(issue_body(route), encoding="utf-8")
    args = ["issue", "create", "--title", route["title"], "--body-file", str(body_path)]
    assignee = os.environ.get(cfg["git"].get("assignee_env", ""))
    if assignee:
        args += ["--assignee", assignee]
    url = _gh(repo, *args).strip().splitlines()[-1]
    m = re.search(r"/issues/(\d+)$", url)
    if not m:
        raise BranchError(f"이슈 번호를 확인할 수 없다: {url}")
    log(f"이슈 생성: {url}")
    return int(m.group(1))


def _base_ref(repo, base):
    """기준 브랜치 ref. 로컬 브랜치가 뒤처져 있을 수 있어 origin 을 먼저 쓴다."""
    # 오프라인 등으로 fetch 가 실패해도 로컬 ref 로 계속 진행한다
    guard.git(repo, "fetch", "origin", base, check=False)
    for ref in (f"origin/{base}", base):
        if guard.git(repo, "rev-parse", "--verify", "--quiet", ref, check=False).returncode == 0:
            return ref
    raise BranchError(f"기준 브랜치 {base} 를 찾을 수 없다")


def checkout(repo, branch, base=None):
    """branch 로 이동한다. 없으면 base 에서 만들고, origin 에만 있으면 추적 브랜치로 만든다."""
    if guard.current_branch(repo) == branch:
        return
    local = guard.git(repo, "rev-parse", "--verify", "--quiet", f"refs/heads/{branch}", check=False).returncode == 0
    if local:
        guard.git(repo, "checkout", branch)
    elif guard.git(repo, "rev-parse", "--verify", "--quiet", f"refs/remotes/origin/{branch}",
                   check=False).returncode == 0:
        guard.git(repo, "checkout", "-b", branch, "--track", f"origin/{branch}")
    else:
        guard.git(repo, "checkout", "-b", branch, _base_ref(repo, base), "--no-track")


def prepare(cfg, agents, repo, goal, run_dir, assume_yes=False, log=print):
    """목표에 맞는 브랜치로 이동하고 {"branch", "issue", "action"} 을 반환한다."""
    issues, branches = list_issues(repo), list_branches(repo)
    log("Router 호출")
    route, cost = agents.route(route_prompt(goal, issues, branches))
    log(f"Router: {route['match']} - {route['reason']}")
    action = decide(route, issues, branches)
    base = cfg["git"].get("base_branch", "dev")

    issue = None
    if action[0] == "checkout":
        branch = action[1]
        m = re.match(r"feature/(\d+)-", branch)
        issue = int(m.group(1)) if m else None
    elif action[0] == "create_branch":
        issue, branch = action[1], action[2]
    else:
        issue = _create_issue(repo, cfg, action[1], run_dir / "issue-body.md", assume_yes, log)
        branch = f"feature/{issue}-{_slugify(action[1].get('slug'))}"

    checkout(repo, branch, base)
    log(f"브랜치: {branch}" + (f" (이슈 #{issue})" if issue else ""))
    return {"branch": branch, "issue": issue, "action": action[0], "cost_usd": cost}
