---
name: create-pr
description: .github/PULL_REQUEST_TEMPLATE/prTemplate.md 양식에 맞춰 Pull Request를 작성하고 gh CLI로 등록한다. "PR 만들어줘", "PR 올려줘", "풀리퀘스트 생성" 등의 요청에 사용.
---

# PR 생성 스킬

`.github/PULL_REQUEST_TEMPLATE/prTemplate.md` 양식을 그대로 따라 PR을 만들고 `gh`로 등록한다.

## 절차

1. **양식 읽기**: `.github/PULL_REQUEST_TEMPLATE/prTemplate.md`를 Read로 읽는다. 실제 파일을 기준으로 하며, 이 문서의 요약은 참고용이다.
2. **상태 확인** (병렬로 실행):
   - `git status` — 커밋되지 않은 변경이 있으면 사용자에게 알리고 커밋 여부를 묻는다. 임의로 커밋하지 않는다.
   - `git branch --show-current` — 현재 브랜치. `main`/`dev` 위에서는 PR을 만들지 않고 사용자에게 알린다.
   - `git log dev..HEAD --oneline` 및 `git diff dev...HEAD --stat` — PR에 포함될 변경 파악. (기준 브랜치가 `dev`가 아니라고 사용자가 말하면 그에 따른다.)
   - `gh pr list --head <현재브랜치>` — 이미 열린 PR이 있으면 새로 만들지 않고 알린다.
3. **변경 내용 분석**: 커밋 메시지와 diff(`git diff dev...HEAD`)를 읽고 실제 변경을 요약한다. 커밋 메시지만 믿지 말고 diff로 확인한다.
4. **관련 이슈 찾기**:
   - 브랜치 이름 `feature/<이슈번호>-...`에서 번호를 추출하고, 커밋 메시지의 `(#N)`도 참고한다.
   - `gh issue view <번호>`로 존재/내용을 확인한다.
   - 이슈를 완전히 해결하면 `Closes #N`, 일부만 관련되면 `Related #N`. 해당 없는 줄은 비워 두지 말고 삭제하거나 "없음"으로 한다. 번호를 추측하지 않는다.
5. **제목 작성**: 저장소 커밋 컨벤션과 맞춘 `Type: 설명` 형식을 쓰되, 이슈 제목 접두사가 `[Feat]` 형식이면 그에 맞춘다. 70자 이내, 한국어.
   - 예: `Feat: Post 서비스 계층 구현 (#15)`
6. **본문 작성**: 템플릿의 섹션 구조·이모지·`---` 구분선을 그대로 유지하고 한국어로 채운다.
   - `🔗 Related Issue`: 위에서 확인한 번호
   - `✨ Summary`: 무엇을 왜 바꿨는지 2~4줄 bullet
   - `📝 Changes`: `Added / Changed / Fixed / Removed` 4개 소제목을 모두 유지. 해당 없으면 `-` 대신 `없음`
   - `🧪 How to Test`: 환경(OS, JDK, DB), 재현 가능한 Test Steps, Expected Result. JDK/DB 버전은 `build.gradle`/`pom.xml`/`application*.yml`에서 확인하고, 모르면 추측하지 말고 사용자에게 묻거나 비워 둔다.
   - `📷 Screenshots (Optional)`: UI 변경이 없으면 "해당 없음"
   - `📚 Additional Notes`: 리뷰어가 알아야 할 설계 판단, 남은 과제
   - `✅ Checklist`: 실제로 확인한 항목만 `[x]`. 테스트를 실행하지 않았다면 체크하지 말고, 필요하면 테스트를 먼저 돌려 결과를 근거로 체크한다.
   - 템플릿의 `<!-- 안내 주석 -->`은 실제 내용으로 교체한다.
   - 본문 마지막 줄에 시스템이 지시하는 attribution 문구가 있으면 덧붙인다.
7. **푸시 확인**: `git rev-parse --abbrev-ref @{u}`와 `git status -sb`로 원격 반영 여부를 확인한다. 푸시가 안 되어 있으면 PR 생성 전에 필요하다고 알리고, 사용자 승인 후 `git push -u origin <현재브랜치>`(origin의 feature 브랜치)로 푸시한다. `main`/`dev`로 직접 푸시하지 않는다. 강제 푸시 금지.
8. **사용자 확인**: 기준 브랜치, 제목, 본문을 보여주고 승인을 받은 뒤 등록한다. 사용자가 "바로 올려"라고 명시했으면 생략 가능.
9. **등록**: 본문을 스크래치패드의 임시 파일에 UTF-8로 저장하고 `--body-file`로 전달한다.

   ```bash
   gh pr create --base dev --head <현재브랜치> --title "<제목>" --body-file <본문파일> --assignee conradrado [--label <라벨>]
   ```

   라벨은 `gh label list`에 존재하는 것만 사용하고, 없으면 생략한다. 템플릿 기본 라벨 `documentation`은 문서 변경일 때만 쓴다.
10. **결과 보고**: PR 번호와 URL을 알려준다.

## 주의

- 기준(base) 브랜치는 `.claude/project.md`에 따라 기본 `dev`이다.
- 커밋·푸시는 사용자가 요청하거나 승인한 경우에만 한다.
- 본문에 존재하지 않는 테스트 결과, 이슈 번호, 환경 정보를 지어내지 않는다.
