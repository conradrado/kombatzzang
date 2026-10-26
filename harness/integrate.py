"""병렬로 만든 브랜치를 push 전에 통합한다. 최신 base 위로 rebase 하고, 겹치는 형제 브랜치는 merge 하며,
충돌은 Resolver 에이전트가 푼다. 통합으로 코드가 바뀌었으면 테스트를 다시 돌린다.

통합은 락 안에서 한 번에 하나씩 한다. 나중에 끝난 브랜치가 먼저 끝난 브랜치를 흡수하므로 머지 시점의 깜짝 충돌이 없다.
"""
import re
import threading

import guard
import branching
import verify
from agents import ClaudeAgents

MAX_RESOLVE_STEPS = 10  # rebase 는 커밋마다 충돌할 수 있어 한 번의 통합에서 Resolver 를 부르는 상한
MARKER = re.compile(r"^(<{7}|>{7})(\s|$)", re.M)


class IntegrationError(RuntimeError):
    pass


def conflicted_files(path):
    return guard.git(path, "diff", "--name-only", "--diff-filter=U").stdout.split()


def head_sha(path):
    return guard.git(path, "rev-parse", "HEAD").stdout.strip()


def conflicts_with(path, other):
    """HEAD 와 other 를 merge 하면 충돌하는지 작업 트리를 건드리지 않고 확인한다. (git >= 2.38)"""
    return guard.git(path, "merge-tree", "--write-tree", "--no-messages", "HEAD", other, check=False).returncode == 1


def resolve_prompt(kind, branch, other, title, files):
    if kind == "rebase":
        side = (f"`HEAD` 쪽은 최신 {other} 에 이미 들어간 코드이고, 반대쪽은 이슈 브랜치 {branch} 의 커밋이다. "
                "(리베이스라 ours/theirs 가 뒤바뀌어 있다)")
    else:
        side = f"`HEAD` 쪽은 이슈 브랜치 {branch} 의 코드이고, 반대쪽은 먼저 끝난 형제 브랜치 {other} 의 코드다."
    return (f"브랜치 {branch} (이슈: {title}) 를 통합하다 충돌이 났다. {side}\n"
            "양쪽 의도를 모두 살려 아래 파일의 충돌을 풀어라.\n\n충돌 파일:\n" + "\n".join(f"- {f}" for f in files))


class Integrator:
    def __init__(self, cfg, base, agents_factory=None, verify_fn=None, log=print):
        self.cfg, self.base, self.log = cfg, base, log
        self.agents_factory = agents_factory or (lambda path: ClaudeAgents(cfg, repo_root=path))
        self.verify_fn = verify_fn or verify.run_verify
        self.lock = threading.Lock()
        self.done = []  # 통합을 마친 (이슈 번호, 브랜치)
        self.cost = 0.0

    def _resolve(self, path, kind, branch, other, title):
        """충돌 파일을 Resolver 로 풀고 git add 한다. 마커가 남았으면 실패시킨다."""
        files = conflicted_files(path)
        if not files:
            raise IntegrationError("충돌 파일이 없는데 git 이 실패했다")
        try:
            data, cost = self.agents_factory(path).resolve(resolve_prompt(kind, branch, other, title, files), files)
        except RuntimeError as e:
            raise IntegrationError(f"Resolver 실패: {e}") from e
        self.cost += cost
        left = []
        for f in files:
            full = path / f
            if full.is_file() and MARKER.search(full.read_text(encoding="utf-8", errors="replace")):
                left.append(f)
        if left:
            raise IntegrationError(f"충돌 마커가 남음: {', '.join(left)}")
        guard.git(path, "add", "--", *files)
        self.log(f"  {branch}: 충돌 {len(files)}개 해결 ({data['summary'][:80]})")

    def _rebase(self, path, branch, base_ref, title):
        proc = guard.git(path, "rebase", base_ref, check=False)
        for _ in range(MAX_RESOLVE_STEPS):
            if proc.returncode == 0:
                return
            try:
                self._resolve(path, "rebase", branch, base_ref, title)
            except IntegrationError:
                guard.git(path, "rebase", "--abort", check=False)
                raise
            proc = guard.git(path, "-c", "core.editor=true", "rebase", "--continue", check=False)
        guard.git(path, "rebase", "--abort", check=False)
        raise IntegrationError(f"rebase 충돌 해결이 {MAX_RESOLVE_STEPS}단계 안에 끝나지 않음")

    def _merge(self, path, branch, sibling, title):
        proc = guard.git(path, "merge", "--no-edit", sibling, check=False)
        if proc.returncode == 0:
            return
        try:
            self._resolve(path, "merge", branch, sibling, title)
            guard.git(path, "-c", "core.editor=true", "commit", "--no-edit")
        except (IntegrationError, RuntimeError):
            guard.git(path, "merge", "--abort", check=False)
            raise

    def integrate(self, issue, path, branch, title):
        """branch 를 최신 base 와 먼저 끝난 형제 브랜치 위로 통합한다. {"changed", "merged"} 반환."""
        with self.lock:
            base_ref = branching._base_ref(path, self.base)
            before = head_sha(path)
            self._rebase(path, branch, base_ref, title)
            merged = []
            for sib_issue, sib_branch in self.done:
                if conflicts_with(path, sib_branch):
                    self._merge(path, branch, sib_branch, title)
                    merged.append(sib_branch)
            changed = head_sha(path) != before
            if changed:
                log_path = path / "harness" / "runs" / f"integration-{issue}-verify.log"
                result = self.verify_fn(self.cfg, {}, path, log_path)
                if not result.passed:
                    raise IntegrationError(f"통합 후 테스트 실패 (로그: {log_path})")
            self.done.append((issue, branch))
            return {"changed": changed, "merged": merged}
