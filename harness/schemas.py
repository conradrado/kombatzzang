"""에이전트 출력 스키마. claude --json-schema 에 그대로 전달하고, 파이썬에서도 다시 검증한다."""

PLAN_SCHEMA = {
    "type": "object",
    "required": ["summary", "tasks", "acceptance_criteria", "allowed_paths"],
    "properties": {
        "summary": {"type": "string"},
        "tasks": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["id", "title", "description"],
                "properties": {
                    "id": {"type": "string"},
                    "title": {"type": "string"},
                    "description": {"type": "string"},
                },
            },
        },
        "acceptance_criteria": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["id", "description"],
                "properties": {
                    "id": {"type": "string"},
                    "description": {"type": "string"},
                },
            },
        },
        "allowed_paths": {"type": "array", "items": {"type": "string"}},
        "test_filter": {"type": "array", "items": {"type": "string"}},
    },
}

GENERATOR_SCHEMA = {
    "type": "object",
    "required": ["summary", "changed_files"],
    "properties": {
        "summary": {"type": "string"},
        "changed_files": {"type": "array", "items": {"type": "string"}},
        "notes": {"type": "string"},
    },
}

EVALUATION_SCHEMA = {
    "type": "object",
    "required": ["verdict", "criteria", "feedback"],
    "properties": {
        "verdict": {"type": "string", "enum": ["PASS", "FAIL"]},
        "criteria": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["id", "met", "evidence"],
                "properties": {
                    "id": {"type": "string"},
                    "met": {"type": "boolean"},
                    "evidence": {"type": "string"},
                },
            },
        },
        "feedback": {"type": "string"},
        "blocking_issues": {"type": "array", "items": {"type": "string"}},
    },
}

ROUTE_SCHEMA = {
    "type": "object",
    "required": ["match", "reason"],
    "properties": {
        "match": {"type": "string", "enum": ["branch", "issue", "none"]},
        "reason": {"type": "string"},
        "branch": {"type": "string"},         # match=branch: 후보 브랜치 이름 그대로
        "issue_number": {"type": "integer"},  # match=issue: 후보 이슈 번호
        "slug": {"type": "string"},           # match=issue/none: 브랜치 이름용 영문 소문자-하이픈
        "title": {"type": "string"},          # match=none: 새 이슈 제목 ([Feat] Title - Description)
        "description": {"type": "string"},
        "goal": {"type": "string"},
        "tasks": {"type": "array", "items": {"type": "string"}},
    },
}

COMMIT_TYPES = ["Feat", "Fix", "Refactor", "Test", "Docs", "Chore"]

COMMIT_SCHEMA = {
    "type": "object",
    "required": ["commits"],
    "properties": {
        "commits": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["type", "subject", "files"],
                "properties": {
                    "type": {"type": "string", "enum": COMMIT_TYPES},
                    "subject": {"type": "string"},  # "도메인 - 설명" (type 접두사와 이슈 번호는 하네스가 붙인다)
                    "body": {"type": "string"},
                    "files": {"type": "array", "items": {"type": "string"}},
                },
            },
        },
    },
}

PR_SCHEMA = {
    "type": "object",
    "required": ["title", "body"],
    "properties": {
        "title": {"type": "string"},
        "body": {"type": "string"},
    },
}

_TYPES = {
    "object": dict,
    "array": list,
    "string": str,
    "boolean": bool,
    "integer": int,
}


class SchemaError(ValueError):
    pass


def validate(data, schema, path="$"):
    t = schema.get("type")
    if t and not isinstance(data, _TYPES[t]):
        raise SchemaError(f"{path}: {t} 타입이어야 함")
    if "enum" in schema and data not in schema["enum"]:
        raise SchemaError(f"{path}: {schema['enum']} 중 하나여야 함")
    if t == "object":
        for key in schema.get("required", []):
            if key not in data:
                raise SchemaError(f"{path}.{key}: 필수 필드 누락")
        for key, sub in schema.get("properties", {}).items():
            if key in data:
                validate(data[key], sub, f"{path}.{key}")
    elif t == "array" and "items" in schema:
        for i, item in enumerate(data):
            validate(item, schema["items"], f"{path}[{i}]")
    return data
