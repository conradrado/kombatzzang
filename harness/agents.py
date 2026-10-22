"""claude -p CLI 로 Planner / Generator / Evaluator 를 호출한다."""
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

from config import HARNESS_DIR, PROMPTS_DIR, REPO_ROOT
from schemas import (EVALUATION_SCHEMA, GENERATOR_SCHEMA, PLAN_SCHEMA,
                     ROUTE_SCHEMA, SchemaError, validate)

# Planner / Evaluator 는 코드를 수정하지 않으므로 읽기 도구만 열어 준다.
READ_ONLY_TOOLS = "Read,Grep,Glob"
# Generator 가 사용할 수 있는 도구 목록. (--tools: 사용 가능한 도구 자체를 제한)
GENERATOR_TOOLS = "Read,Grep,Glob,Edit,Write,Bash"
# Generator 가 확인 없이 실행할 수 있는 도구/명령. Bash 는 git 읽기 명령과 gradle 만 허용한다.
GENERATOR_ALLOWED = [
    "Read", "Grep", "Glob", "Edit", "Write",
    "Bash(git diff:*)", "Bash(git status:*)", "Bash(git log:*)",
    "Bash(./gradlew:*)", "Bash(gradlew:*)", "Bash(gradlew.bat:*)",
]
# Generator 가 절대 실행할 수 없는 명령. 커밋/푸시/브랜치 조작과 파일 삭제, GitHub CLI 를 막는다.
GENERATOR_DISALLOWED = [
    "Bash(git push:*)", "Bash(git commit:*)", "Bash(git checkout:*)",
    "Bash(git reset:*)", "Bash(git rebase:*)", "Bash(git merge:*)",
    "Bash(rm:*)", "Bash(gh:*)",
]


class AgentError(RuntimeError):
    """claude CLI 호출 실패(시간 초과, 출력 파싱 실패, 스키마 위반 등)를 나타낸다."""
    pass


class ClaudeAgents:
    """역할별(planner/generator/evaluator)로 claude CLI 를 서브프로세스로 호출하는 래퍼."""

    def __init__(self, cfg, repo_root=REPO_ROOT):
        self.cfg = cfg
        self.repo_root = repo_root
        self.claude = shutil.which("claude")  # claude 실행 파일 경로 탐색
        if not self.claude:
            raise AgentError("claude CLI 를 PATH 에서 찾을 수 없음")

    def route(self, prompt):
        """목표에 대응하는 기존 이슈/브랜치가 있는지 판정한다(ROUTE_SCHEMA). 읽기 전용."""
        return self._call("router", prompt, ROUTE_SCHEMA, READ_ONLY_TOOLS)

    def plan(self, prompt):
        """목표를 분석해 계획(PLAN_SCHEMA)을 만든다. 읽기 전용."""
        return self._call("planner", prompt, PLAN_SCHEMA, READ_ONLY_TOOLS)

    def generate(self, prompt, allowed_paths=None):
        """allowed_paths 가 있으면 PreToolUse 훅이 그 밖의 쓰기를 호출 시점에 거부한다."""
        # 허용 경로가 없으면 훅 없이 호출한다. (사후 검사는 guard.py 가 담당)
        if allowed_paths is None:
            return self._call("generator", prompt, GENERATOR_SCHEMA, GENERATOR_TOOLS,
                              allowed=GENERATOR_ALLOWED, disallowed=GENERATOR_DISALLOWED)
        # 훅은 별도 프로세스라 메모리를 공유할 수 없으므로, 허용 경로를 환경변수(JSON 문자열)로 전달한다.
        scope = json.dumps({"repo_root": str(self.repo_root), "allowed_paths": list(allowed_paths)},
                           ensure_ascii=False)
        return self._call("generator", prompt, GENERATOR_SCHEMA, GENERATOR_TOOLS,
                          allowed=GENERATOR_ALLOWED, disallowed=GENERATOR_DISALLOWED,
                          settings=self._scope_hook_settings(),
                          env_extra={"HARNESS_SCOPE": scope})

    @staticmethod
    def _scope_hook_settings():
        """scope_guard.py 를 Edit/Write/Bash 의 PreToolUse 훅으로 등록하는 --settings JSON."""
        hook = (HARNESS_DIR / "hooks" / "scope_guard.py").as_posix()
        command = f'"{Path(sys.executable).as_posix()}" "{hook}"'  # 훅 명령은 bash 로 실행되므로 / 경로 사용
        return json.dumps({"hooks": {"PreToolUse": [{
            "matcher": "Edit|Write|NotebookEdit|Bash",
            "hooks": [{"type": "command", "command": command}],
        }]}})

    def evaluate(self, prompt):
        """Generator 결과를 평가해 판정(EVALUATION_SCHEMA)을 만든다. 읽기 전용."""
        return self._call("evaluator", prompt, EVALUATION_SCHEMA, READ_ONLY_TOOLS)

    def _call(self, role, prompt, schema, tools, allowed=None, disallowed=None,
              attempts=2, settings=None, env_extra=None):
        """claude -p 를 실행해 (검증된 구조화 출력, 사용 비용 USD) 를 반환한다.

        role: 역할 이름. prompts/{role}.md 가 시스템 프롬프트로 추가되고 config 의 모델명도 이 키로 찾는다.
        schema: 출력 JSON 스키마. CLI 에 강제하고, 받은 결과도 validate() 로 다시 검증한다.
        tools: 사용 가능한 도구 목록.
        allowed / disallowed: 허용 / 금지 도구 규칙 목록.
        attempts: 출력 파싱 실패, claude 오류, 스키마 위반 시 재시도할 총 횟수. (시간 초과는 재시도하지 않음)
        settings: --settings 로 넘길 JSON 문자열. (훅 등록용)
        env_extra: 서브프로세스 환경변수에 추가할 값.
        """
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
        if settings:
            cmd += ["--settings", settings]
        env = {**os.environ, **(env_extra or {})}
        # config.toml 의 [models] 에 역할별 모델이 지정돼 있으면 사용한다.
        model = self.cfg.get("models", {}).get(role)
        if model:
            cmd += ["--model", model]

        cost = 0.0  # 재시도 비용까지 누적
        last_err = None
        for attempt in range(1, attempts + 1):
            # 프롬프트는 길 수 있으므로 인자 대신 stdin 으로 전달 (Windows 명령줄 길이 제한 회피)
            try:
                proc = subprocess.run(
                    cmd, input=prompt, capture_output=True, text=True,
                    encoding="utf-8", errors="replace", cwd=self.repo_root, env=env,
                    timeout=limits["agent_timeout_sec"],
                )
            except subprocess.TimeoutExpired as e:
                raise AgentError(f"{role}: 시간 초과 ({limits['agent_timeout_sec']}s)") from e
            # 1) CLI 의 JSON 출력 자체를 파싱한다.
            try:
                out = json.loads(proc.stdout)
            except json.JSONDecodeError:
                last_err = f"{role}: JSON 출력 파싱 실패: {proc.stdout[:200]!r} {proc.stderr[:200]!r}"
                continue
            cost += out.get("total_cost_usd") or 0.0
            # 2) claude 가 오류로 끝났는지 확인한다. (예산 초과 등)
            if out.get("is_error"):
                last_err = f"{role}: claude 오류 ({out.get('subtype')}): {str(out.get('result'))[:300]}"
                continue
            # 3) --json-schema 로 요청한 구조화 출력이 있는지 확인한다.
            data = out.get("structured_output")
            if data is None:
                last_err = f"{role}: structured_output 없음"
                continue
            # 4) 하네스 쪽에서도 스키마를 한 번 더 검증한다.
            try:
                validate(data, schema)
            except SchemaError as e:
                last_err = f"{role}: 스키마 위반: {e}"
                continue
            return data, cost
        # 모든 시도가 실패하면 마지막 실패 사유로 예외를 던진다. (loop.py 가 상태를 저장하고 --resume 가능)
        raise AgentError(last_err)
