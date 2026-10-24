# Planner / Generator / Evaluator 루프 하네스

목표가 달성될 때까지 3개의 `claude -p` 에이전트를 반복 호출한다. 이슈 #73.

```
python harness/run.py "<목표>"              # 새 run
python harness/run.py --goal-file goal.md
python harness/run.py --resume <run-id>     # 중단된 run 이어서
python harness/run.py "<목표>" --no-branch   # 이슈/브랜치 탐색 없이 현재 브랜치에서 실행
python harness/run.py "<목표>" --no-commit   # 성공해도 커밋/PR 초안을 만들지 않음
python harness/run.py "<목표>" --yes         # 새 이슈 등록을 확인 없이 승인
python harness/tests/test_harness.py        # 단위 테스트 (claude/gradle 불필요)
```

종료 코드: `0` 성공 / `2` 중단(한도 초과 등) / 그 외 오류.

## 병렬 실행 (`parallel.py`)
```
python harness/parallel.py 93 92            # 지정한 이슈를 worktree 로 나눠 병렬 구현
python harness/parallel.py --auto 2         # 브랜치/담당자 없는 열린 이슈 중 최신순 2개 선택(확인 프롬프트)
python harness/parallel.py 93 92 --no-push  # 커밋까지만
python harness/parallel.py 93 --cleanup     # push 성공한 worktree 삭제
```
이슈마다 `<저장소>-wt-<번호>` worktree 와 `feature/<번호>-<slug>` 브랜치(`origin/dev` 기준)를 만들고 그 안에서 `run.py --no-branch` 를 돌린다. 종료 코드 0(테스트 green + Evaluator PASS + 커밋 완료)이고 dev 보다 앞선 커밋이 있는 브랜치만 **오케스트레이터가** `git push -u origin` 한다. 에이전트의 push/gh 금지는 그대로다. PR 은 등록하지 않고 초안을 `runs/parallel-*/issue-N-pr-*` 로 모은다. `--workers`(기본 3)만큼 동시에 돌고, 비용 상한은 이슈 수 × `max_total_cost_usd` 다. `.env` 같은 gitignore 파일은 worktree 로 복사되지 않는다.

## 동작
0. **Router** (새 run 만, `branching.py`): 열린 이슈와 브랜치 목록을 보고 목표와 같은 작업이 있는지 판정한다. 후보에 없는 값은 거부한다.
   - 브랜치 있음 → `checkout` (origin 에만 있으면 추적 브랜치로 생성)
   - 이슈만 있음 → `feature/<번호>-<slug>` 를 `base_branch`(dev) 에서 생성. 같은 번호의 브랜치가 이미 있으면 그것으로 이동
   - 둘 다 없음 → 이슈 템플릿 양식으로 이슈 등록(확인 프롬프트, `--yes` 로 생략. 담당자는 환경변수 `HARNESS_ASSIGNEE`, 없으면 지정 안 함) 후 브랜치 생성. 비대화형이면 `--yes` 없이는 중단
   - 이동 후에 baseline 을 찍고 보호 브랜치 검사를 한다. `--resume` 은 현재 브랜치를 그대로 쓴다.
1. **Planner** (읽기 전용): `plan.json` — tasks, acceptance criteria, 수정 허용 경로, 테스트 필터
2. **Generator**: 코드 수정. git 상태 변경 명령은 차단. `Edit`/`Write`/`Bash` 호출 직전에 PreToolUse 훅(`hooks/scope_guard.py`)이 허용 경로 밖 쓰기를 거부한다
3. **가드**: 훅이 놓친 허용 경로 밖 변경을 사후에 되돌림 (`.git/ .github/ .claude/ harness/` 는 항상 금지)
4. **Verify**: 하네스가 `gradlew test` 를 직접 실행
5. **Evaluator** (읽기 전용, 별도 세션): criteria 별 PASS/FAIL + 피드백

6. **Publish** (성공한 run 만, `publish.py`, `--no-commit` 으로 생략): 이번 run 에서 바뀐 범위 안 파일을 모아 **Committer**(읽기 전용)에게 논리 단위 묶음을 받는다. 모든 파일이 정확히 한 커밋에 한 번씩 들어갔는지 하네스가 검증하고(실패 시 사유를 알려 1회 재시도), `Type: 도메인 - 설명 (#이슈)` 제목으로 순서대로 `git commit --only` 한다. 이어서 **PR Writer**(읽기 전용)가 `prTemplate.md` 양식의 제목/본문을 `pr-title.txt` `pr-body.md` 로 남긴다. push 와 PR 등록은 하지 않고, 등록 명령만 출력한다.

성공 = 테스트 green **그리고** Evaluator PASS **그리고** 모든 criteria 충족 **그리고** 범위 위반 없음.

같은 실패가 연속 2회 반복되면 Planner 를 다시 부르고(`max_replans`), 그래도 반복되면 중단한다.
최대 반복 횟수와 비용 상한은 `config.toml` 에서 조정한다.

## 안전장치
- `main` / `dev` 브랜치에서는 실행을 거부한다.
- 에이전트(Generator 등)는 commit / push / `gh` 를 쓸 수 없다. 커밋은 성공한 run 의 Publish 단계에서 **하네스가** 만들고, push 와 PR 등록은 하지 않는다. 실패하거나 `--no-commit` 이면 결과가 작업 트리에 남는다.
- Generator 의 쓰기 범위는 두 겹으로 막는다. 1차는 PreToolUse 훅(호출 시점 거부), 2차는 `guard.py` 의 사후 되돌리기다. 훅은 호출마다 `--settings` 로만 주입하므로 `.claude/settings.json` 과 일반 Claude Code 세션에는 영향이 없다.
- 호출마다 `--no-session-persistence --strict-mcp-config --disable-slash-commands` 로 컨텍스트를 줄인다.

## 산출물 (`harness/runs/<run-id>/`, gitignore 대상)
`goal.md` `plan.json` `state.json` `result.json` `iter-NN/{generator.json, diff.patch, verify.log, evaluation.json}` `commits.json` `pr-title.txt` `pr-body.md`

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
| `hooks/scope_guard.py` | Generator 전용 PreToolUse 훅. 허용 경로 밖 `Edit`/`Write`와 파일을 쓰는 `Bash` 명령을 호출 시점에 거부 |
| `publish.py` | 성공 후 변경 수집, Committer 묶음 검증, 단위 커밋 생성, PR 초안 저장 |
| `verify.py` | `gradlew test` 를 직접 실행하고 종료 코드로 통과 여부 판정 |
| `prompts/*.md` | 역할별 시스템 프롬프트 (`planner.md` `generator.md` `evaluator.md` `router.md` `committer.md` `pr_writer.md`) |

### 의존 관계
```
run.py
 ├─ config.load_config()          → config.toml 을 dict(cfg) 로 반환
 ├─ guard.current_branch()        → 보호 브랜치 검사
 ├─ ClaudeAgents(cfg)             → agents.py
 │    ├─ config.PROMPTS_DIR, REPO_ROOT 상수를 import
 │    ├─ schemas.PLAN/GENERATOR/EVALUATION_SCHEMA, validate() 를 import
 │    └─ (generate 시) claude 가 hooks/scope_guard.py 를 별도 프로세스로 실행 → 훅이 guard.in_scope() 재사용
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
   1. **Generator**: `state["feedback"]` 를 포함한 프롬프트와 `plan["allowed_paths"]` 로 `agents.generate()` 를 호출한다. 이 동안 PreToolUse 훅이 허용 경로 밖 쓰기를 거부한다. (아래 "쓰기 범위 훅" 참고) → `generator.json`
   2. **범위 검사** (훅이 놓친 변경의 안전망): `guard.snapshot()` 을 다시 찍어 `baseline` 과 비교(`changed_since`)하고, `split_violations` 로 `plan["allowed_paths"]` 안/밖을 나눈다. 밖의 변경은 `guard.revert()` 가 되돌린다. 추적 파일은 `git checkout HEAD -- <path>`, 새 파일은 삭제한다. `baseline` 부터 더러웠던 파일은 복원할 수 없어 `unreverted` 로만 보고한다. 범위 안 변경은 `guard.build_diff()` 로 diff 를 만든다. → `diff.patch`
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
2. 허용/차단 도구가 있으면 `--allowedTools` / `--disallowedTools` 를 붙인다. `generate()` 에서 `allowed_paths` 가 넘어오면 `--settings`(훅 등록 JSON)도 붙이고 환경변수 `HARNESS_SCOPE` 를 추가한다.
3. `cfg["models"][role]` 이 비어 있지 않으면 `--model` 을 붙인다. 없거나 빈 문자열이면 CLI 기본 모델을 쓴다.
4. 프롬프트는 인자가 아니라 **stdin** 으로 전달한다. (Windows 명령줄 길이 제한 회피) 작업 디렉터리는 `REPO_ROOT`, 제한 시간은 `agent_timeout_sec` 이다.
5. 응답 JSON 을 검사한다. 아래 중 하나에 걸리면 `last_err` 를 기록하고 재시도한다. (최대 `attempts=2`)
   - JSON 파싱 실패
   - `is_error` 가 true
   - `structured_output` 이 없음
   - `schemas.validate()` 의 스키마 위반
6. 호출마다 `total_cost_usd` 를 `cost` 에 더한다. 재시도 비용도 포함한다. 성공하면 `(data, cost)` 를, 모두 실패하면 `AgentError` 를 던진다. 시간 초과는 재시도 없이 바로 `AgentError` 다.

### 쓰기 범위 훅 (`hooks/scope_guard.py`)
Generator 가 허용 경로 밖 파일을 쓰려는 순간 호출 시점에 막는다. 쓴 뒤에 되돌리는 `guard.py` 와 달리 거부 사유가 모델에게 바로 돌아가므로 같은 턴에서 스스로 고칠 수 있다.

1. `ClaudeAgents.generate()` 가 허용 경로와 `repo_root` 를 JSON 으로 만들어 환경변수 `HARNESS_SCOPE` 에 담는다.
2. `_scope_hook_settings()` 가 `Edit|Write|NotebookEdit|Bash` 의 PreToolUse 훅으로 `scope_guard.py` 를 등록하는 JSON 문자열을 반환하고, `_call` 이 이를 `--settings` 로 넘긴다. 등록은 claude CLI 가 하며 그 호출 하나에만 적용된다.
3. Generator 가 해당 도구를 쓰려 할 때마다 claude 가 `scope_guard.py` 를 별도 프로세스로 실행한다. 이 프로세스는 환경변수를 상속받으므로 `HARNESS_SCOPE` 로 허용 경로를 읽는다.
4. 판정:
   - `Edit`/`Write`/`NotebookEdit`: 경로를 저장소 기준 상대 경로로 바꿔 `guard.in_scope()` 로 검사한다. 저장소 밖 경로(`..` 탈출)와 `ALWAYS_FORBIDDEN` 도 거부한다.
   - `Bash`: `>` 리다이렉션, `tee`, `sed -i`, `mv`, `cp`, `rm`, `Set-Content` 등 쓰기 패턴이 보이면 거부한다. `2>&1` 과 `/dev/null` 은 통과시킨다. 문자열 매칭이라 완전하지 않고 오탐이 있을 수 있다.
   - 거부는 stdout 으로 `permissionDecision: "deny"` JSON 을 내서 한다. 통과는 아무것도 출력하지 않는다.
5. `HARNESS_SCOPE` 가 없으면 아무것도 막지 않는다. 하네스 밖에서 실행돼도 일반 작업을 방해하지 않는다.

훅이 놓친 변경(Bash 로 우회한 쓰기 등)은 위 "범위 검사" 단계가 사후에 되돌린다.

인증은 하네스가 다루지 않는다. `claude` CLI 의 로그인 상태 또는 환경 변수 `ANTHROPIC_API_KEY` 를 CLI 가 그대로 사용한다.
