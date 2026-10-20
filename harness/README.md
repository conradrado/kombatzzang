# Planner / Generator / Evaluator 루프 하네스

목표가 달성될 때까지 3개의 `claude -p` 에이전트를 반복 호출한다. 이슈 #73.

```
python harness/run.py "<목표>"              # 새 run
python harness/run.py --goal-file goal.md
python harness/run.py --resume <run-id>     # 중단된 run 이어서
python harness/tests/test_harness.py        # 단위 테스트 (claude/gradle 불필요)
```

종료 코드: `0` 성공 / `2` 중단(한도 초과 등) / 그 외 오류.

## 동작
1. **Planner** (읽기 전용): `plan.json` — tasks, acceptance criteria, 수정 허용 경로, 테스트 필터
2. **Generator**: 코드 수정. git 상태 변경 명령은 차단
3. **가드**: 허용 경로 밖 변경은 되돌림 (`.git/ .github/ .claude/ harness/` 는 항상 금지)
4. **Verify**: 하네스가 `gradlew test` 를 직접 실행
5. **Evaluator** (읽기 전용, 별도 세션): criteria 별 PASS/FAIL + 피드백

성공 = 테스트 green **그리고** Evaluator PASS **그리고** 모든 criteria 충족 **그리고** 범위 위반 없음.

같은 실패가 연속 2회 반복되면 Planner 를 다시 부르고(`max_replans`), 그래도 반복되면 중단한다.
최대 반복 횟수와 비용 상한은 `config.toml` 에서 조정한다.

## 안전장치
- `main` / `dev` 브랜치에서는 실행을 거부한다.
- 하네스는 commit / push 를 하지 않는다. 결과는 작업 트리에 남으므로 직접 검토 후 커밋한다.
- 호출마다 `--no-session-persistence --strict-mcp-config --disable-slash-commands` 로 컨텍스트를 줄인다.

## 산출물 (`harness/runs/<run-id>/`, gitignore 대상)
`goal.md` `plan.json` `state.json` `result.json` `iter-NN/{generator.json, diff.patch, verify.log, evaluation.json}`

## 내부 동작 흐름

### 파일별 역할
| 파일 | 역할 |
|---|---|
| `run.py` | CLI 진입점. 인자 파싱, 설정 로드, 보호 브랜치 검사, `Harness` 실행, 종료 코드 결정 |
| `config.py` | 경로 상수(`HARNESS_DIR` `REPO_ROOT` `RUNS_DIR` `PROMPTS_DIR`)와 `load_config()` |
| `config.toml` | `[limits]` `[verify]` `[git]` `[models]` 설정값 |
| `loop.py` | `Harness` 클래스. Planner → (Generator → 가드 → Verify → Evaluator) 반복의 상태 머신 |
| `agents.py` | `ClaudeAgents` 클래스. `claude -p` subprocess 호출, 재시도, 출력 검증 |
| `schemas.py` | Plan / Generator / Evaluation JSON 스키마와 `validate()` |
| `guard.py` | git 스냅샷으로 변경 파일을 추적하고, 허용 경로 밖 변경을 되돌림 |
| `verify.py` | `gradlew test` 를 직접 실행하고 종료 코드로 통과 여부 판정 |
| `prompts/*.md` | 역할별 시스템 프롬프트 (`planner.md` `generator.md` `evaluator.md`) |

### 의존 관계
```
run.py
 ├─ config.load_config()          → config.toml 을 dict(cfg) 로 반환
 ├─ guard.current_branch()        → 보호 브랜치 검사
 ├─ ClaudeAgents(cfg)             → agents.py
 │    ├─ config.PROMPTS_DIR, REPO_ROOT 상수를 import
 │    └─ schemas.PLAN/GENERATOR/EVALUATION_SCHEMA, validate() 를 import
 └─ Harness(cfg, agents, REPO_ROOT, run_dir)   → loop.py
      ├─ guard.*                  (스냅샷, 범위 검사, 되돌리기, diff)
      └─ verify.run_verify        (테스트 실행)
```
- `load_config()` 를 호출하는 곳은 `run.py` 뿐이다. `agents.py` 는 `config.py` 의 함수를 실행하지 않고, 상수만 import 한다.
- `cfg` dict 는 `run.py` 가 만들어서 `ClaudeAgents(cfg)` 와 `Harness(cfg, ...)` 생성자에 그대로 넘긴다. 각 클래스가 `self.cfg` 로 보관하고, `agents.py` 는 `cfg["limits"]` `cfg["models"]`, `loop.py` 는 `cfg["limits"]`, `verify.py` 는 `cfg["verify"]` `cfg["limits"]` 를 읽는다.
- `agents.py`, `loop.py`, `verify.py` 는 서로 직접 호출하지 않는다. 호출 순서는 `loop.py` 의 `Harness` 가 정한다. 테스트에서는 `agents` 와 `verifier` 를 가짜로 바꿔 끼울 수 있다.

### 실행 순서
1. **시작 (`run.py:main`)**
   1. stdout/stderr 를 UTF-8 로 재설정한다. (Windows cp949 콘솔 대응)
   2. `argparse` 로 `goal`, `--goal-file`, `--resume`, `--config`, `--max-iterations` 를 파싱한다.
   3. `load_config()` 로 `config.toml` 을 읽고, `--max-iterations` 가 있으면 `cfg["limits"]["max_iterations"]` 를 덮어쓴다.
   4. `guard.current_branch()` 가 `cfg["git"]["protected_branches"]` 에 속하면 `sys.exit` 로 거부한다.
   5. `ClaudeAgents(cfg)` 를 만든다. 이때 `shutil.which("claude")` 로 CLI 를 찾고, 없으면 `AgentError`.
   6. `--resume` 이면 `runs/<run-id>/state.json` 을 `Harness.load()` 로 읽고 `status` 를 `RUNNING` 으로 바꾼다. 아니면 목표를 `--goal-file` 또는 위치 인자에서 받아 `runs/<YYYYMMDD-HHMMSS>/` 를 만들고 `Harness.new_state(goal)` 을 호출한다.
   7. `Harness.run(state)` 를 실행하고, 결과 `status` 에 따라 종료 코드를 정한다. (`SUCCESS` 0 / `ABORTED` 2 / 그 외 1)

2. **상태 초기화 (`Harness.new_state`)**
   - `state` 에는 `goal`, `status`, `plan`, `iteration`, `replans`, `cost_usd`, `feedback`, `last_signature`, `baseline`, `reason` 이 들어간다.
   - `baseline` 은 `guard.snapshot()` 결과다. 하네스 시작 시점에 이미 더러웠던 파일의 `경로 → 내용 해시` 이고, 이후 "에이전트가 바꾼 파일"을 가려내는 기준이 된다.

3. **계획 (`Harness._plan`)**
   - 목표(재계획이면 이전 계획과 실패 요약 포함)로 프롬프트를 만들어 `agents.plan()` 을 호출한다.
   - 반환된 plan 은 `state["plan"]` 에 저장하고 `plan.json` (재계획은 `plan.replanN.json`) 으로 기록한다. 비용은 `state["cost_usd"]` 에 누적한다.
   - `--resume` 으로 `plan` 이 이미 있으면 이 단계를 건너뛴다.

4. **반복 (`Harness.run` 의 while → `Harness._iterate`)**
   반복 시작 전에 비용 상한(`max_total_cost_usd`)을 확인하고, 넘었으면 `ABORTED` 로 끝낸다. 반복마다 `runs/<run-id>/iter-NN/` 을 만든다.
   1. **Generator**: `state["feedback"]` 를 포함한 프롬프트로 `agents.generate()` 를 호출한다. → `generator.json`
   2. **범위 검사**: `guard.snapshot()` 을 다시 찍어 `baseline` 과 비교(`changed_since`)하고, `split_violations` 로 `plan["allowed_paths"]` 안/밖을 나눈다. 밖의 변경은 `guard.revert()` 가 되돌린다. 추적 파일은 `git checkout HEAD -- <path>`, 새 파일은 삭제한다. `baseline` 부터 더러웠던 파일은 복원할 수 없어 `unreverted` 로만 보고한다. 범위 안 변경은 `guard.build_diff()` 로 diff 를 만든다. → `diff.patch`
   3. **Verify**: `verifier`(기본 `verify.run_verify`)가 `gradlew test [--tests <필터>...]` 를 직접 실행한다. 종료 코드 0 이고 시간 초과가 아니면 통과다. 로그 전체는 `verify.log`, 끝부분(`log_tail_chars`)과 실패 라인(최대 30줄)은 `VerifyResult` 로 돌려준다.
   4. **Evaluator**: 목표, acceptance criteria, Generator 보고, 테스트 결과, 범위 위반, diff 를 담아 `agents.evaluate()` 를 호출한다. → `evaluation.json`
   5. **판정**: 아래 네 조건이 모두 참이면 성공이다.
      - 테스트 통과
      - Evaluator `verdict == "PASS"`
      - 미충족 criteria 없음 (`_unmet_criteria`)
      - 범위 위반 없음

5. **실패 처리**
   - `_signature()` 가 (테스트 통과 여부, 실패 라인, 차단 이슈, 위반 파일)을 해시한다. 숫자는 `#` 로 치환해 줄 번호나 시간 차이를 무시한다.
   - 직전 반복의 `last_signature` 와 같으면 "정체(stagnant)"로 본다.
   - 정체이면 `replans < max_replans` 일 때 `replans` 를 올리고 Planner 를 `_plan(replan_reason=feedback)` 으로 다시 부른다. 한도를 다 썼으면 `ABORTED`.
   - 정체가 아니면 `_compose_feedback()` 이 Evaluator 피드백, 차단 이슈, 미충족 criteria, 테스트 로그 끝부분, 범위 위반 안내를 합쳐 `state["feedback"]` 에 넣고 다음 반복의 Generator 에게 전달한다.
   - 반복마다 끝에서 `state.json` 을 저장하므로 중단돼도 `--resume` 으로 이어갈 수 있다.

6. **종료 (`Harness._finish`)**
   - `status` 와 `reason` 을 `state.json` 에 저장하고, `status` `reason` `iteration` `replans` `cost_usd` 를 `result.json` 에 기록한다.
   - `SUCCESS` 는 성공 조건 충족, `ABORTED` 는 비용 상한, 최대 반복 횟수, 반복 정체 + replan 소진 중 하나다.
   - `AgentError` 등 예외가 나면 `state["reason"]` 에 남기고 상태를 저장한 뒤 다시 던진다. `run.py` 는 `--resume` 안내와 함께 종료한다.

### 에이전트 호출 상세 (`ClaudeAgents._call`)
`plan` / `generate` / `evaluate` 는 모두 `_call(role, prompt, schema, tools, ...)` 로 모인다.

| 역할 | 스키마 | 허용 도구 |
|---|---|---|
| planner | `PLAN_SCHEMA` | `Read,Grep,Glob` (읽기 전용) |
| generator | `GENERATOR_SCHEMA` | `Read,Grep,Glob,Edit,Write,Bash` 중 `GENERATOR_ALLOWED` 만 허용. `git commit/push/checkout/reset/rebase/merge`, `rm`, `gh` 는 `GENERATOR_DISALLOWED` 로 차단 |
| evaluator | `EVALUATION_SCHEMA` | `Read,Grep,Glob` (읽기 전용) |

1. 명령어를 조립한다: `claude -p --output-format json --json-schema <스키마> --append-system-prompt-file prompts/<role>.md --tools ... --max-budget-usd <max_cost_per_call_usd> --no-session-persistence --strict-mcp-config --disable-slash-commands`
2. 허용/차단 도구가 있으면 `--allowedTools` / `--disallowedTools` 를 붙인다.
3. `cfg["models"][role]` 이 비어 있지 않으면 `--model` 을 붙인다. 없거나 빈 문자열이면 CLI 기본 모델을 쓴다.
4. 프롬프트는 인자가 아니라 **stdin** 으로 전달한다. (Windows 명령줄 길이 제한 회피) 작업 디렉터리는 `REPO_ROOT`, 제한 시간은 `agent_timeout_sec` 이다.
5. 응답 JSON 을 검사한다. 아래 중 하나에 걸리면 `last_err` 를 기록하고 재시도한다. (최대 `attempts=2`)
   - JSON 파싱 실패
   - `is_error` 가 true
   - `structured_output` 이 없음
   - `schemas.validate()` 의 스키마 위반
6. 호출마다 `total_cost_usd` 를 `cost` 에 더한다. 재시도 비용도 포함한다. 성공하면 `(data, cost)` 를, 모두 실패하면 `AgentError` 를 던진다. 시간 초과는 재시도 없이 바로 `AgentError` 다.

인증은 하네스가 다루지 않는다. `claude` CLI 의 로그인 상태 또는 환경 변수 `ANTHROPIC_API_KEY` 를 CLI 가 그대로 사용한다.
