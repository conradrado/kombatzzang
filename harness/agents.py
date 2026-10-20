"""claude -p CLI 로 Planner / Generator / Evaluator 를 호출한다."""
import json
import shutil
import subprocess

from config import PROMPTS_DIR, REPO_ROOT
from schemas import (EVALUATION_SCHEMA, GENERATOR_SCHEMA, PLAN_SCHEMA,
                     SchemaError, validate)

READ_ONLY_TOOLS = "Read,Grep,Glob"
GENERATOR_TOOLS = "Read,Grep,Glob,Edit,Write,Bash"
GENERATOR_ALLOWED = [
    "Read", "Grep", "Glob", "Edit", "Write",
    "Bash(git diff:*)", "Bash(git status:*)", "Bash(git log:*)",
    "Bash(./gradlew:*)", "Bash(gradlew:*)", "Bash(gradlew.bat:*)",
]
GENERATOR_DISALLOWED = [
    "Bash(git push:*)", "Bash(git commit:*)", "Bash(git checkout:*)",
    "Bash(git reset:*)", "Bash(git rebase:*)", "Bash(git merge:*)",
    "Bash(rm:*)", "Bash(gh:*)",
]


class AgentError(RuntimeError):
    pass


class ClaudeAgents:
    def __init__(self, cfg, repo_root=REPO_ROOT):
        self.cfg = cfg
        self.repo_root = repo_root
        self.claude = shutil.which("claude")
        if not self.claude:
            raise AgentError("claude CLI 를 PATH 에서 찾을 수 없음")

    def plan(self, prompt):
        return self._call("planner", prompt, PLAN_SCHEMA, READ_ONLY_TOOLS)

    def generate(self, prompt):
        return self._call("generator", prompt, GENERATOR_SCHEMA, GENERATOR_TOOLS,
                          allowed=GENERATOR_ALLOWED, disallowed=GENERATOR_DISALLOWED)

    def evaluate(self, prompt):
        return self._call("evaluator", prompt, EVALUATION_SCHEMA, READ_ONLY_TOOLS)

    def _call(self, role, prompt, schema, tools, allowed=None, disallowed=None,
              attempts=2):
        limits = self.cfg["limits"]
        cmd = [
            self.claude, "-p",
            "--output-format", "json",
            "--json-schema", json.dumps(schema),
            "--append-system-prompt-file", str(PROMPTS_DIR / f"{role}.md"),
            "--tools", tools,
            "--max-budget-usd", str(limits["max_cost_per_call_usd"]),
            # 호출마다 독립 세션 + 불필요한 컨텍스트(MCP, 스킬) 제거로 비용 절감
            "--no-session-persistence",
            "--strict-mcp-config",
            "--disable-slash-commands",
        ]
        if allowed:
            cmd += ["--allowedTools", ",".join(allowed)]
        if disallowed:
            cmd += ["--disallowedTools", ",".join(disallowed)]
        model = self.cfg.get("models", {}).get(role)
        if model:
            cmd += ["--model", model]

        cost = 0.0
        last_err = None
        for attempt in range(1, attempts + 1):
            # 프롬프트는 길 수 있으므로 인자 대신 stdin 으로 전달 (Windows 명령줄 길이 제한 회피)
            try:
                proc = subprocess.run(
                    cmd, input=prompt, capture_output=True, text=True,
                    encoding="utf-8", errors="replace", cwd=self.repo_root,
                    timeout=limits["agent_timeout_sec"],
                )
            except subprocess.TimeoutExpired as e:
                raise AgentError(f"{role}: 시간 초과 ({limits['agent_timeout_sec']}s)") from e
            try:
                out = json.loads(proc.stdout)
            except json.JSONDecodeError:
                last_err = f"{role}: JSON 출력 파싱 실패: {proc.stdout[:200]!r} {proc.stderr[:200]!r}"
                continue
            cost += out.get("total_cost_usd") or 0.0
            if out.get("is_error"):
                last_err = f"{role}: claude 오류 ({out.get('subtype')}): {str(out.get('result'))[:300]}"
                continue
            data = out.get("structured_output")
            if data is None:
                last_err = f"{role}: structured_output 없음"
                continue
            try:
                validate(data, schema)
            except SchemaError as e:
                last_err = f"{role}: 스키마 위반: {e}"
                continue
            return data, cost
        raise AgentError(last_err)
