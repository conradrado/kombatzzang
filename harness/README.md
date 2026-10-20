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
