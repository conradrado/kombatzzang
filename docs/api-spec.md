# Kombatzzang API 명세

## 1. 개요

- 명세 형식: Swagger/OpenAPI (springdoc-openapi)
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- 이 문서는 컨트롤러·DTO 코드 기준으로 정리한 요약본이며, 상세 스키마는 Swagger UI에서 확인할 수 있다.

## 2. 인증

- 인증 방식: Bearer JWT. 요청 헤더에 `Authorization: Bearer {accessToken}` 을 포함한다.
- 인증 없이 호출 가능한 엔드포인트: `POST /api/users`, `/api/auth/**`, Swagger UI(`/swagger-ui.html`, `/v3/api-docs`)
- 그 외 모든 엔드포인트는 인증이 필요하다.
- 권한 표기: `인증` = 로그인한 사용자 누구나, `본인/ADMIN` 등은 `@PreAuthorize` 기준이다.

## 3. 공통 응답

### ErrorResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| status | int | HTTP 상태 코드 |
| error | String | 상태 코드 사유 문구 (예: `Bad Request`) |
| message | String | 오류 메시지 |

### 상태 코드 매핑 (GlobalExceptionHandler 기준)

| 상태 코드 | 발생 조건 |
|---|---|
| 400 | 요청 본문 검증 실패(`MethodArgumentNotValidException`), 필수 쿼리 파라미터 누락, `IllegalArgumentException` |
| 401 | 미인증, 로그인 정보 불일치(`InvalidCredentialsException`), 유효하지 않은 Refresh Token(`InvalidRefreshTokenException`) |
| 403 | 권한 없음(`AccessDeniedException`) |
| 404 | 대상 없음 (`UserNotFoundException`, `PostNotFoundException`, `BattleNotFoundException`) |
| 409 | 중복(`DuplicateUsernameException`, `DuplicateEmailException`), `IllegalStateException` |
| 500 | 그 외 처리되지 않은 예외 |

### CursorPageResponse\<T\>

| 필드 | 타입 | 설명 |
|---|---|---|
| content | List\<T\> | 조회 결과 |
| nextCursor | Long | 다음 페이지 요청에 사용할 커서 |
| hasNext | boolean | 다음 페이지 존재 여부 |

## 4. 도메인별 API

### 4.1 Auth

#### POST /api/auth/login

- 설명: 이메일/비밀번호로 로그인하여 Access/Refresh Token을 발급한다.
- 권한: 비인증 허용
- Request Body: `LoginRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| email | String | `@NotBlank`, `@Email` |
| password | String | `@NotBlank` |

- Response: `200 OK` `TokenResponse`

| 필드 | 타입 |
|---|---|
| accessToken | String |
| refreshToken | String |
| tokenType | String (`Bearer`) |
| expiresIn | long (초 단위) |

#### POST /api/auth/refresh

- 설명: Refresh Token으로 새로운 Access/Refresh Token을 발급한다.
- 권한: 비인증 허용
- Request Body: `RefreshRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| refreshToken | String | `@NotBlank` |

- Response: `200 OK` `TokenResponse`

#### POST /api/auth/logout

- 설명: Refresh Token을 폐기한다.
- 권한: 비인증 허용
- Request Body: `RefreshRequest` (위와 동일)
- Response: `204 No Content`

### 4.2 User

#### POST /api/users

- 설명: 새로운 사용자를 등록한다.
- 권한: 비인증 허용
- Request Body: `UserCreateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| username | String | `@NotBlank`, `@Size(min = 2, max = 20)` |
| email | String | `@NotBlank`, `@Email` |
| password | String | `@NotBlank`, `@Size(min = 8, max = 64)` |
| profileImageKey | String | 없음 |

- Response: `201 Created` `UserResponse`

#### GET /api/users/{userId}

- 설명: 사용자 단건 조회
- 권한: 인증
- Path: `userId` (Long)
- Response: `200 OK` `UserResponse`

#### GET /api/users

- 설명: 커서 기반 페이지네이션으로 사용자 목록을 조회한다.
- 권한: 인증
- Query: `cursor` (Long, 선택, 이전 페이지 마지막 사용자 ID), `size` (int, 기본값 20)
- Response: `200 OK` `CursorPageResponse<UserResponse>`

#### GET /api/users/active

- 설명: 활동 지역이 일치하는 사용자 목록을 커서 기반으로 조회한다.
- 권한: 인증
- Query: `activityRegion` (Region, 필수), `cursor` (Long, 선택), `size` (int, 기본값 20)
- Response: `200 OK` `CursorPageResponse<UserResponse>`

#### PATCH /api/users/{userId}

- 설명: 사용자 정보를 수정한다.
- 권한: 본인 또는 ADMIN
- Path: `userId` (Long)
- Request Body: `UserUpdateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| username | String | 없음 |
| profileImageKey | String | 없음 |
| sport | Sport | 없음 |
| weightClass | WeightClass | 없음 |
| activityRegion | Region | 없음 |
| bio | String | `@Size(max = 500)` |

- Response: `200 OK` `UserResponse`

#### PATCH /api/users/{userId}/record

- 설명: 사용자 전적/레벨을 수정한다.
- 권한: ADMIN 전용
- Path: `userId` (Long)
- Request Body: `UserRecordUpdateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| wins | Integer | `@Min(0)` |
| losses | Integer | `@Min(0)` |
| level | UserLevel | 없음 |

- Response: `200 OK` `UserResponse`

#### PATCH /api/users/{userId}/region

- 설명: 사용자 지역을 변경한다.
- 권한: 본인 또는 ADMIN
- Path: `userId` (Long)
- Request Body: `UserRegionUpdateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| region | Region | `@NotNull` |

- Response: `200 OK` `UserResponse`

#### DELETE /api/users/{userId}

- 설명: 사용자를 삭제한다.
- 권한: 본인 또는 ADMIN
- Path: `userId` (Long)
- Response: `204 No Content`

#### UserResponse

| 필드 | 타입 |
|---|---|
| id | Long |
| username | String |
| email | String |
| profileImageKey | String |
| role | UserRole |
| region | Region |
| sport | Sport |
| weightClass | WeightClass |
| activityRegion | Region |
| wins | int |
| losses | int |
| level | UserLevel |
| bio | String |
| createdAt | Instant |
| updatedAt | Instant |

### 4.3 Post

#### POST /api/posts

- 설명: 로그인한 사용자를 작성자로 게시글을 등록한다.
- 권한: 인증
- Request Body: `PostCreateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| title | String | `@NotBlank`, `@Size(max = 255)` |
| body | String | 없음 |

- Response: `201 Created` `PostResponse`

#### PATCH /api/posts/{postId}

- 설명: 게시글을 수정한다.
- 권한: 작성자 본인 또는 ADMIN
- Path: `postId` (Long)
- Request Body: `PostUpdateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| title | String | `@Size(max = 255)` |
| body | String | 없음 |

- Response: `200 OK` `PostResponse`

#### DELETE /api/posts/{postId}

- 설명: 게시글을 삭제한다.
- 권한: ADMIN 전용
- Path: `postId` (Long)
- Response: `204 No Content`

#### PostResponse

| 필드 | 타입 |
|---|---|
| id | Long |
| title | String |
| body | String |
| userId | Long |
| status | PostStatus |
| createdAt | Instant |

### 4.4 Battle

#### POST /api/battles

- 설명: 로그인한 사용자를 신청자로 상대에게 대전을 신청한다.
- 권한: 인증
- Request Body: `BattleCreateCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| opponentId | Long | `@NotNull` |
| sport | Sport | `@NotNull` |
| scheduledAt | Instant | 없음 |
| location | String | `@Size(max = 255)` |
| message | String | 없음 |

- Response: `201 Created` `BattleResponse`

#### GET /api/battles/{battleId}

- 설명: 대전을 조회한다.
- 권한: 대전 참가자 또는 ADMIN
- Path: `battleId` (Long)
- Response: `200 OK` `BattleResponse`

#### PATCH /api/battles/{battleId}/accept

- 설명: 대전을 수락한다.
- 권한: 상대방 본인 또는 ADMIN
- Path: `battleId` (Long)
- Response: `200 OK` `BattleResponse`

#### PATCH /api/battles/{battleId}/reject

- 설명: 대전을 거절한다.
- 권한: 상대방 본인 또는 ADMIN
- Path: `battleId` (Long)
- Response: `200 OK` `BattleResponse`

#### PATCH /api/battles/{battleId}/cancel

- 설명: 대전을 취소한다.
- 권한: 신청자 본인 또는 ADMIN
- Path: `battleId` (Long)
- Response: `200 OK` `BattleResponse`

#### PATCH /api/battles/{battleId}/result

- 설명: 대전 결과(승자)를 기록한다.
- 권한: 대전 참가자 또는 ADMIN
- Path: `battleId` (Long)
- Request Body: `BattleResultCommand`

| 필드 | 타입 | 제약 |
|---|---|---|
| winnerId | Long | `@NotNull` |

- Response: `200 OK` `BattleResponse`

#### DELETE /api/battles/{battleId}

- 설명: 대전 전적을 삭제한다.
- 권한: ADMIN 전용
- Path: `battleId` (Long)
- Response: `204 No Content`

#### GET /api/battles/subscribe

- 설명: SSE로 대전 신청 알림을 실시간으로 받는다.
- 권한: 인증
- Response: `200 OK` `text/event-stream` (SSE)

#### BattleResponse

| 필드 | 타입 |
|---|---|
| id | Long |
| challengerId | Long |
| opponentId | Long |
| sport | Sport |
| scheduledAt | Instant |
| location | String |
| message | String |
| status | BattleStatus |
| winnerId | Long |
| createdAt | Instant |

### 4.5 전적

별도의 전적 도메인/엔드포인트는 없으며 다음 두 API로 다룬다.

- `PATCH /api/users/{userId}/record` : 전적(wins, losses)과 레벨을 직접 수정 (ADMIN)
- `PATCH /api/battles/{battleId}/result` : 대전 결과 기록 (참가자 또는 ADMIN)

### 4.6 Follow

미구현 - 추후 작성

## 부록: Enum 값

| Enum | 값 |
|---|---|
| Region | SEOUL, BUSAN, DAEGU, INCHEON, GWANGJU, DAEJEON, ULSAN, SEJONG, GYEONGGI, GANGWON, CHUNGBUK, CHUNGNAM, JEONBUK, JEONNAM, GYEONGBUK, GYEONGNAM, JEJU, UNKNOWN |
| Sport | MMA, BOXING, MUAY_THAI, BJJ, KICKBOXING, WRESTLING, UNKNOWN |
| WeightClass | STRAW, FEATHER, LIGHT, WELTER, MIDDLE, LIGHT_HEAVY, HEAVY, INFINITE, UNKNOWN |
| UserLevel | BEGINNER, AMATEUR, INTERMEDIATE, ADVANCED, PRO |
| UserRole | ADMIN, MODERATOR, MEMBER |
| BattleStatus | REQUESTED, ACCEPTED, REJECTED, CANCELED, COMPLETED |
| PostStatus | RECRUITING, CLOSED |
