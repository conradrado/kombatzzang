"""Generator 의 PreToolUse 훅. 허용 경로 밖 쓰기를 도구 호출 시점에 거부한다.

claude -p 호출 때 agents.py 가 --settings 로 주입하며, 허용 경로는 환경변수
HARNESS_SCOPE 환경변수의 JSON({"repo_root", "allowed_paths"})에서 읽는다.
환경변수가 없으면(하네스 밖 세션) 아무것도 막지 않고 통과시킨다.
"""
import json
import os
import re
import sys
from pathlib import Path

# 어느 위치에서 실행돼도 harness 폴더의 guard 를 import 할 수 있도록 경로에 추가
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import guard

WRITE_TOOLS = ("Edit", "Write", "NotebookEdit")

# Bash 로 파일을 쓰는 흔한 패턴. (2>&1, &>, >&N, /dev/null 리다이렉션은 제외)
BASH_WRITE_PATTERNS = (
    re.compile(r"(?<![0-9&>])>{1,2}\s*(?!&|/dev/null)\S"),
    re.compile(r"\btee\b"),
    re.compile(r"\bsed\s+(-[A-Za-z]*i|--in-place)"),
    re.compile(r"\b(mv|cp|rm|touch|truncate|dd)\b"),
    re.compile(r"\b(Set-Content|Add-Content|Out-File|Remove-Item|Move-Item|Copy-Item)\b", re.I),
)


def deny(reason):
    sys.stdout.reconfigure(encoding="utf-8")  # Windows 기본 인코딩(cp949)으로 한글 사유가 깨지는 것을 방지
    json.dump({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}, sys.stdout, ensure_ascii=False)


def check_path(file_path, scope):
    """허용이면 None, 거부면 사유 문자열."""
    root = Path(scope["repo_root"]).resolve()
    target = Path(file_path)
    if not target.is_absolute():
        target = root / target
    try:
        rel = target.resolve().relative_to(root).as_posix()
    except ValueError:
        return f"저장소 밖 경로는 수정할 수 없음: {file_path}"
    if guard.in_scope(rel, scope["allowed_paths"]):
        return None
    return (f"허용 경로 밖이라 수정할 수 없음: {rel}. "
            f"허용 경로: {', '.join(scope['allowed_paths'])} "
            f"(.git/ .github/ .claude/ harness/ 는 항상 금지)")


def check_bash(command):
    for pattern in BASH_WRITE_PATTERNS:
        if pattern.search(command):
            return ("Bash 로 파일을 쓰거나 지울 수 없음. 파일 수정은 Edit/Write 도구를 쓸 것. "
                    f"(차단된 명령: {command[:120]})")
    return None


def decide(event, scope):
    """훅 입력 이벤트에 대한 거부 사유. 통과면 None."""
    tool, tool_input = event.get("tool_name"), event.get("tool_input") or {}
    if tool in WRITE_TOOLS:
        path = tool_input.get("file_path") or tool_input.get("notebook_path")
        return check_path(path, scope) if path else None
    if tool == "Bash":
        return check_bash(tool_input.get("command", ""))
    return None


def main():
    raw_scope = os.environ.get("HARNESS_SCOPE")
    if not raw_scope:
        return
    scope = json.loads(raw_scope)
    reason = decide(json.load(sys.stdin), scope)
    if reason:
        deny(reason)


if __name__ == "__main__":
    main()
