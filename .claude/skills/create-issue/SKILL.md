---
name: create-issue
description: .github/ISSUE_TEMPLATE/issue-template.md 양식에 맞춰 GitHub 이슈를 작성하고 gh CLI로 등록한다. "이슈 만들어줘", "이슈 생성", "이슈 올려줘" 등의 요청이나 작업에 대응하는 이슈가 없을 때 사용.
---

# 이슈 생성 스킬

`.github/ISSUE_TEMPLATE/issue-template.md` 양식을 그대로 따라 이슈를 만들고 `gh`로 등록한다.

## 절차

1. **양식 읽기**: `.github/ISSUE_TEMPLATE/issue-template.md`를 Read로 읽는다. 양식이 바뀌었을 수 있으니 이 문서의 예시가 아니라 실제 파일을 기준으로 한다.
2. **내용 파악**: 사용자 요청, 대화 맥락, 필요하면 `git log`/`git diff`로 작업 내용을 파악한다. 불분명한 핵심(무엇을, 왜)만 사용자에게 묻고 나머지는 합리적으로 채운다.
3. **중복 확인**: `gh issue list --state open --search "<키워드>"`로 이미 같은 이슈가 있는지 확인한다. 있으면 새로 만들지 말고 사용자에게 알린다.
4. **제목 작성**: 양식의 `[Feat] Title - Description` 형식을 따른다.
   - 접두사는 작업 성격에 맞게: `[Feat]`, `[Fix]`, `[Refactor]`, `[Test]`, `[Docs]`, `[Chore]`
   - 예: `[Feat] Post 서비스 계층 구현 - 게시글 CRUD`
5. **본문 작성**: front matter(`---`로 둘러싸인 `name/about/title/labels/assignees`)는 **제외**하고, `# 📌 Issue`부터 템플릿의 섹션 구조·이모지·`---` 구분선을 그대로 유지한다. 한국어로 작성한다.
   - `📖 Description`: 작업 내용
   - `🎯 Goal`: 달성하려는 목표
   - `✅ Tasks`: `- [ ] 항목` 체크리스트 (빈 항목 남기지 말 것, 구체적인 단위 작업으로)
   - `📎 References (Optional)`: 참고 자료가 없으면 섹션은 유지하되 "없음"으로 둔다
   - 템플릿의 `<!-- 안내 주석 -->`은 실제 내용으로 교체한다.
6. **라벨/담당자**: 템플릿 front matter의 `assignees`(conradrado)를 `--assignee`로 지정한다. 라벨은 `gh label list`로 저장소에 존재하는 라벨 중 작업 성격에 맞는 것을 고르고, 템플릿 기본값(`documentation`)은 실제로 문서 작업일 때만 쓴다. 맞는 라벨이 없으면 라벨 없이 만든다(새 라벨 생성 금지).
7. **사용자 확인**: 등록은 외부에 공개되는 동작이므로, 제목·본문·라벨·담당자를 보여주고 승인을 받은 뒤에 등록한다. 사용자가 "바로 올려"라고 명시했으면 생략 가능.
8. **등록**: 본문을 임시 파일(스크래치패드)에 UTF-8로 저장하고 `--body-file`로 전달한다. 셸 인용 문제와 한글 깨짐을 피하기 위함이다.

   ```bash
   gh issue create --title "<제목>" --body-file <본문파일> --assignee conradrado [--label <라벨>]
   ```

9. **결과 보고**: 이슈 번호와 URL을 알려준다.

## 브랜치 (사용자가 요청한 경우에만)

`.claude/project.md` 규칙을 따른다.
- 기준: `dev` 브랜치에서 checkout
- 이름: `feature/이슈번호-도메인-계층 및 작업내용` (예: `feature/18-post-exception`)
- push는 사용자가 요청할 때만 한다.

## 주의

- 이슈 번호를 추측하지 않는다. 등록 결과로 받은 번호만 사용한다.
- 작업 범위가 해당 도메인 패키지를 벗어나는 내용이 Tasks에 들어가면 사용자에게 먼저 묻는다.
