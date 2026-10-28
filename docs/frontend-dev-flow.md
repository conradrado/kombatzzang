# 프론트엔드(React) 개발 흐름

## 1. 개요 및 원칙
- 백엔드 코드(`src/`, `build.gradle` 등)는 **변경하지 않는다.**
- `frontend/`는 저장소 루트의 독립 프로젝트(Vite + React, JavaScript)이며 Gradle 빌드와 무관하다.
- 현재 구현 범위: 로그인 → accessToken 저장 → 인증 API(`GET /api/users`) 호출 → 로그아웃, 유저 프로필 조회/수정, 팔로우/언팔로우, 팔로워·팔로잉 목록, 게시글 피드/작성/수정/삭제(10절).

## 2. 사전 준비
- Node.js 18 이상
- 백엔드 로컬 실행: `./gradlew bootRun` (local 프로필, PostgreSQL 필요). 기본 포트는 8080이다.
- 로그인할 계정이 필요하다 (`POST /api/users`로 가입).

## 3. 디렉터리 구조
```
frontend/
├── index.html
├── package.json
├── vite.config.js
├── .gitignore
└── src/
    ├── main.jsx            # createRoot 진입점
    ├── App.jsx             # 토큰 유무 및 화면 상태(list/profile/follows) 전환
    ├── constants.js        # Region/Sport/WeightClass/UserLevel 값
    ├── api/client.js       # fetch 래퍼 및 API 함수
    └── components/
        ├── LoginForm.jsx
        ├── UserList.jsx
        ├── UserProfile.jsx
        ├── ProfileEditForm.jsx
        ├── FollowList.jsx
        ├── PostFeed.jsx        # 게시글 카드 목록, 작성/수정/삭제 연동
        ├── PostForm.jsx        # 작성·수정 공용 폼
        └── ConfirmModal.jsx    # 삭제 확인 모달
```

## 4. 실행 방법
```
cd frontend
npm install
npm run dev
```
접속: http://localhost:5173 (백엔드가 8080에서 실행 중이어야 한다.)

## 5. 프록시와 CORS
`SecurityConfig`에 CORS 설정이 없으므로 브라우저가 `http://localhost:8080`을 직접 호출하면 막힌다.
그래서 `vite.config.js`의 `server.proxy`가 `/api`를 `http://localhost:8080`으로 전달한다.
프론트 코드는 항상 상대 경로(`/api/...`)만 사용한다. SSE(`/api/battles/subscribe`)도 같은 프록시를 탄다.

## 6. 인증 흐름
1. `POST /api/auth/login` (`{ email, password }`) → `accessToken`, `refreshToken`, `tokenType`, `expiresIn` 반환.
2. `client.js`가 두 토큰을 localStorage에 저장한다.
3. 이후 모든 요청에 `Authorization: Bearer {accessToken}` 헤더를 붙인다.
4. 401 응답은 토큰 만료/무효를 뜻한다. 현재는 에러 메시지만 표시하며, 확장 시 `POST /api/auth/refresh` (`{ refreshToken }`)로 재발급 후 재시도하거나 로그인 화면으로 돌려보내면 된다.
5. 로그아웃: `POST /api/auth/logout` (`{ refreshToken }`) 호출 후 localStorage의 토큰을 삭제한다.

## 7. API 연동 시 참고
- 명세: [`docs/api-spec.md`](api-spec.md), Swagger UI: `/swagger-ui.html` (http://localhost:8080/swagger-ui.html)
- 에러: 백엔드 `ErrorResponse`(`status`, `error`, `message`)의 `message`를 `Error`로 던진다.
- 페이지네이션: `CursorPageResponse` (`content`, `nextCursor`, `hasNext`). `hasNext`가 true이면 `nextCursor`를 `cursor` 쿼리로 넘겨 다음 페이지를 조회한다. (`GET /api/users?cursor=&size=`)

## 8. 기능 추가 흐름
예: 활성 유저 목록(`GET /api/users/active`) 추가
1. `src/api/client.js`에 `fetchActiveUsers()` 같은 함수를 추가하고 `request()`를 사용한다.
2. `src/components/`에 컴포넌트를 작성하고 `useState`/`useEffect`로 데이터를 불러온다.
3. `App.jsx`에서 컴포넌트를 import해 화면에 연결한다.
4. `npm run dev`로 브라우저에서 확인하고, 요청/응답은 Swagger UI와 비교한다.

## 9. 빌드
```
cd frontend
npm run build
```
`frontend/dist`가 생성된다(커밋 대상 아님). 백엔드 static 복사 등 배포 연동은 백엔드 변경이 필요하므로 향후 과제로 남긴다.

## 10. 게시글 화면
로그인 후 `게시글`(기본) / `유저` 탭으로 전환한다.

### API
| 함수 | 요청 |
|---|---|
| `fetchPosts(cursor, size)` | `GET /api/posts?cursor=&size=` |
| `fetchPost(postId)` | `GET /api/posts/{postId}` |
| `createPost({ title, body })` | `POST /api/posts` |
| `updatePost(postId, { title, body })` | `PATCH /api/posts/{postId}` (작성자 본인 또는 ADMIN) |
| `deletePost(postId)` | `DELETE /api/posts/{postId}` (ADMIN 전용) |

- 입력 제한: 제목 255자(작성 시 필수), 본문 10000자.
- `request()`가 던지는 `Error`에는 `status`(HTTP 상태 코드)가 붙는다. 403이면 "권한이 없습니다."를 표시한다.

### 버튼 노출 규칙
- 수정: 작성자 본인(`post.userId === 현재 userId`) 또는 ADMIN.
- 삭제: ADMIN만. 확인 모달(`ConfirmModal`)에서 승인해야 `deletePost`를 호출한다.
- 현재 사용자 정보는 `getCurrentUser()`가 accessToken의 payload를 디코딩해 얻는다. `sub`는 userId, `role`은 권한(ADMIN/USER, 없을 수 있음)이다. 화면 노출용일 뿐 실제 권한 검증은 백엔드가 한다.

### 의존성
- 목록/상세 조회(`GET /api/posts`, `GET /api/posts/{postId}`)는 #131에서 추가된다. 그 전에는 피드가 에러 상태로 표시된다.
- 목록 응답은 `CursorPageResponse`(`content`, `nextCursor`, `hasNext`) 형식을 가정한다.
