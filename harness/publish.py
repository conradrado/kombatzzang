"""성공한 run 의 변경을 논리 단위로 커밋하고 PR 초안(제목/본문 파일)을 만든다. push 와 PR 등록은 하지 않는다."""
import json
import re

import branching
import guard


class PublishError(RuntimeError):
    pass


def issue_of(branch):
    """feature/<번호>-... 브랜치에서 이슈 번호를 뽑는다. 없으면 None."""
    m = re.match(r"feature/(\d+)-", branch)
    return int(m.group(1)) if m else None


def collect_changes(repo, state):
    """이번 run 에서 바뀐 범위 안 파일. baseline 부터 더러웠고 그대로인 파일은 제외된다."""
    changed = guard.changed_since(state["baseline"], guard.snapshot(repo))
    ok, _ = guard.split_violations(changed, state["plan"]["allowed_paths"])
    return ok


def commit_prompt(repo, state, files):
    recent = guard.git(repo, "log", "--format=%s", "-15").stdout.strip()
    return (f"## 목표\n{state['goal']}\n\n"
            f"## 계획 요약\n{state['plan']['summary']}\n\n"
            f"## 변경 파일 (모두 정확히 한 번씩 커밋에 넣을 것)\n" + "\n".join(f"- {f}" for f in files) + "\n\n"
            f"## 최근 커밋 제목 (스타일 참고)\n{recent}\n\n"
            f"## 변경 diff\n```diff\n{guard.build_diff(repo, files)}\n```\n")


def check_commits(commits, files):
    """모든 변경 파일이 정확히 한 커밋에 한 번씩 들어갔는지 검증한다. 어긋나면 PublishError."""
    flat = [f.replace("\\", "/") for c in commits for f in c["files"]]
    expected = set(files)
    problems = []
    if not commits:
        problems.append("커밋이 하나도 없다")
    if unknown := sorted(set(flat) - expected):
        problems.append(f"변경 목록에 없는 파일: {unknown}")
    if missing := sorted(expected - set(flat)):
        problems.append(f"어느 커밋에도 없는 파일: {missing}")
    if dup := sorted({f for f in flat if flat.count(f) > 1}):
        problems.append(f"여러 커밋에 중복된 파일: {dup}")
    if any(not c["files"] for c in commits):
        problems.append("파일이 없는 커밋이 있다")
    if problems:
        raise PublishError("; ".join(problems))


def plan_commits(agents, repo, state, files, attempts=2):
    """Committer 에게 커밋 묶음을 받는다. 검증에 실패하면 사유를 알려 한 번 더 시도한다. (commits, cost) 반환."""
    prompt, cost = commit_prompt(repo, state, files), 0.0
    for attempt in range(attempts):
        out, c = agents.commit_plan(prompt)
        cost += c
        try:
            check_commits(out["commits"], files)
            return out["commits"], cost
        except PublishError as e:
            err = e
            prompt += f"\n## 직전 응답이 거부된 이유 (고쳐서 다시 답할 것)\n{e}\n"
    raise PublishError(f"Committer 응답이 검증을 통과하지 못했다: {err}")


def commit_message(commit, issue):
    """`Type: 도메인 - 설명 (#이슈)` 제목 + 선택적 본문."""
    subject = f"{commit['type']}: {commit['subject'].strip()}"
    if issue and f"(#{issue})" not in subject:
        subject += f" (#{issue})"
    body = (commit.get("body") or "").strip()
    return subject, body


def make_commits(repo, commits, issue, log=print):
    """커밋을 순서대로 만든다. 이미 스테이지돼 있던 다른 파일은 `--only` 로 건드리지 않는다. 만든 커밋 목록 반환."""
    made = []
    for c in commits:
        files = [f.replace("\\", "/") for f in c["files"]]
        subject, body = commit_message(c, issue)
        try:
            guard.git(repo, "add", "--", *files)
            args = ["commit", "-m", subject] + (["-m", body] if body else []) + ["--only", "--", *files]
            guard.git(repo, *args)
        except RuntimeError as e:
            done = ", ".join(m["subject"] for m in made) or "없음"
            raise PublishError(f"커밋 실패 ({subject}): {e}\n이미 만든 커밋: {done}") from e
        sha = guard.git(repo, "rev-parse", "--short", "HEAD").stdout.strip()
        made.append({"sha": sha, "subject": subject, "files": files})
        log(f"커밋 {sha}: {subject} ({len(files)}개 파일)")
    return made


def pr_prompt(cfg, repo, state, branch, issue, made):
    base = cfg["git"].get("base_branch", "dev")
    try:
        stat = guard.git(repo, "diff", "--stat", f"{branching._base_ref(repo, base)}...HEAD").stdout.strip()
    except (branching.BranchError, RuntimeError):
        stat = "(확인 불가)"
    commits = "\n".join(f"- {m['sha']} {m['subject']}\n" + "\n".join(f"    - {f}" for f in m["files"]) for m in made)
    criteria = "\n".join(f"- {c['id']}: {c['description']}" for c in state["plan"]["acceptance_criteria"])
    return (f"## 목표\n{state['goal']}\n\n"
            f"## 브랜치 / 이슈 / 기준 브랜치\n{branch} / {f'#{issue}' if issue else '없음'} / {base}\n\n"
            f"## 커밋\n{commits}\n\n## Acceptance Criteria (Evaluator 통과)\n{criteria}\n\n"
            f"## 하네스 테스트 결과\n통과 (gradlew test, run 성공 조건)\n\n"
            f"## git diff --stat\n```\n{stat}\n```\n")


def publish(cfg, agents, repo, run_dir, state, branch, log=print):
    """변경 수집 -> 커밋 묶기 -> 커밋 -> PR 초안 저장. 사용한 비용(USD)을 반환한다."""
    files = collect_changes(repo, state)
    if not files:
        log("커밋할 변경이 없다")
        return 0.0
    issue = issue_of(branch)
    log(f"Committer 호출 (변경 {len(files)}개 파일)")
    commits, cost = plan_commits(agents, repo, state, files)
    (run_dir / "commits.json").write_text(json.dumps(commits, ensure_ascii=False, indent=2), encoding="utf-8")
    made = make_commits(repo, commits, issue, log)

    log("PR Writer 호출")
    pr, c = agents.write_pr(pr_prompt(cfg, repo, state, branch, issue, made))
    cost += c
    (run_dir / "pr-title.txt").write_text(pr["title"].strip() + "\n", encoding="utf-8")
    (run_dir / "pr-body.md").write_text(pr["body"].strip() + "\n", encoding="utf-8")
    base = cfg["git"].get("base_branch", "dev")
    log(f"PR 초안: {run_dir / 'pr-title.txt'}, {run_dir / 'pr-body.md'}\n"
        f"  등록: git push -u origin {branch} && gh pr create --base {base} --head {branch} "
        f'--title "$(cat {run_dir / "pr-title.txt"})" --body-file {run_dir / "pr-body.md"}')
    return cost
