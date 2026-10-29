# 프론트엔드(React) 개발 흐름

## 1. 개요 및 원칙
- 백엔드 코드(`src/`, `build.gradle` 등)는 **변경하지 않는다.**
- `frontend/`는 저장소 루트의 독립 프로젝트(Vite + React, JavaScript)이며 Gradle 빌드와 무관하다.
- 현재 구현 범위: 회원가입(`POST /api/users`) → 로그인 → accessToken 저장 → 인증 API(`GET /api/users`) 호출 → 로그아웃.
- 회원가입: 닉네임/이메일/비밀번호/비밀번호 확인을 클라이언트에서 검증(백엔드 `UserCreateCommand`와 동일 제약)한 뒤 API를 호출한다. 409(중복)·400 등 서버 오류는 `message`를 표시하고, 성공하면 로그인 화면으로 돌아가 완료 메시지를 보여 준다. 409 응답에 `message`가 없으면 기본 중복 안내 문구를 표시한다.

## 2. 사전 준비
- Node.js 18 이상 (권장: LTS 22, `frontend/.nvmrc`). Node 25 이상은 내장 `localStorage` 가 jsdom 을 가려 테스트가 깨지므로 `NODE_OPTIONS=--no-experimental-webstorage` 를 주고 실행한다.
- 백엔드 로컬 실행: `./gradlew bootRun` (local 프로필, PostgreSQL 필요). 기본 포트는 8080이다.
- 로그인할 계정이 필요하다 (`POST /api/users`로 가입).

## 3. 디렉터리 구조
```
frontend/
├── index.html
├── package.json
├── vite.config.js          # 프록시 및 vitest(test) 설정
├── .nvmrc
├── .gitignore
└── src/
    ├── test/setup.js       # 테스트 공통 설정 (jest-dom 매처)
    ├── main.jsx            # createRoot 진입점
    ├── App.jsx             # 토큰 유무 및 login/signup 화면 전환
    ├── App.test.jsx
    ├── api/client.js       # fetch 래퍼 및 API 함수 (signup 요청에는 인증 헤더를 붙이지 않음)
    ├── api/client.test.js
    ├── test/setup.js       # vitest 설정(jest-dom)
    └── components/
        ├── LoginForm.jsx
        ├── SignupForm.jsx
        ├── SignupForm.test.jsx
        ├── signupValidation.js
        ├── signupValidation.test.js
        └── UserList.jsx
```

## 4. 실행 방법
```
cd frontend
npm install
npm run dev
```
접속: http://localhost:5173 (백엔드가 8080에서 실행 중이어야 한다.)

테스트 (vitest + Testing Library, 백엔드 불필요):
```
cd frontend
npm test
```
테스트 파일은 대상 옆에 `*.test.js(x)` 로 둔다. 테스트 때문에 `package.json` 이나 `vite.config.js` 를 수정할 필요는 없다.

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

## 9. 테스트
Vitest + jsdom + Testing Library를 사용하며 백엔드 없이 `fetch`를 mock해 실행한다.
```
cd frontend
npm install
npm test
```

## 10. 빌드
```
cd frontend
npm run build
```
`frontend/dist`가 생성된다(커밋 대상 아님). 백엔드 static 복사 등 배포 연동은 백엔드 변경이 필요하므로 향후 과제로 남긴다.
