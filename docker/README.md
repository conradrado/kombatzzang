# Docker 개발 환경

`docker-compose.yml` 로 로컬 개발용 스택(PostgreSQL + Spring Boot 앱)을 실행한다.
Kafka, Redis 는 아직 포함되어 있지 않으며, 필요해지는 시점에 같은 방식으로 추가할 예정이다.

## 구성

| 서비스 | 설명 | 이미지 | 포트 |
|---|---|---|---|
| `postgres` | 애플리케이션 DB | `docker/postgres/Dockerfile` (postgres:16-alpine 기반) | `5432` |
| `app` | Spring Boot 애플리케이션 | `docker/app/Dockerfile` (멀티스테이지: gradle 빌드 → JRE 실행) | `8080` |

두 서비스는 `matzzang-net` 브리지 네트워크로 묶여 있고, `app` 은 `postgres` 가
healthy 상태가 될 때까지 기다렸다가 기동한다(`depends_on.condition: service_healthy`).

```
docker-compose.yml
docker/
├── postgres/
│   └── Dockerfile          # postgres:16-alpine
└── app/
    └── Dockerfile          # Spring Boot 앱 빌드/실행 이미지
```

## 사용법

```bash
# 1) 이미지 빌드 + 기동 (백그라운드) — 저장소에 커밋된 .env 의 기본값을 그대로 사용
docker compose up -d --build

# 2) 로그 확인
docker compose logs -f app

# 3) 종료
docker compose down          # 컨테이너/네트워크 제거, DB 볼륨(postgres_data)은 유지
docker compose down -v       # DB 볼륨까지 완전히 삭제하고 싶을 때
```

기동 후 `http://localhost:8080` 으로 앱에 접근할 수 있고, DB는 호스트에서
`localhost:5432` 로 접속할 수 있다 (예: `psql -h localhost -U matzzang -d matzzang`).

## 환경변수 (`.env`)

`docker-compose.yml` 은 `${VAR:-기본값}` 형태로 환경변수를 참조하고, 프로젝트
루트의 `.env` 파일을 compose가 자동으로 읽는다. 팀 전체가 같은 로컬 기본값을
쓰도록 `.env` 자체를 저장소에 커밋해두었다 (자격 증명이 아니라 로컬 전용
더미 값이라 문제 없음). 값을 바꾸고 싶으면 로컬에서 직접 수정하거나
`.env.local` 등 별도 파일을 만들어 `docker compose --env-file .env.local ...`
로 오버라이드한다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | 앱 컨테이너에 적용할 Spring 프로파일 |
| `DB_NAME` | `matzzang` | DB 이름 |
| `DB_USERNAME` | `matzzang` | DB 계정 |
| `DB_PASSWORD` | `matzzang` | DB 비밀번호 |
| `DB_PORT` | `5432` | 호스트에 노출할 postgres 포트 |
| `APP_PORT` | `8080` | 호스트에 노출할 앱 포트 |

앱 컨테이너 내부에서는 `postgres` 서비스명으로 접속하므로 `DB_HOST=postgres`,
내부 포트는 항상 `5432` 로 고정되어 있다 (호스트 매핑용 `DB_PORT` 와는 별개).

`application-local.properties` 도 동일한 이름의 환경변수를 참조하도록 설정되어
있어서, 컨테이너 밖에서 `./gradlew bootRun` 으로 로컬 실행할 때도 `DB_HOST` 등을
지정하지 않으면 `localhost:5432` 로 접속을 시도한다. 즉 로컬에서
`docker compose up postgres` 로 DB만 띄워두고 앱은 IDE/gradle로 직접 돌리는
방식도 그대로 동작한다.

## Spring 프로파일 (local / prod)

`application.properties` 는 공통 설정만 담고, 환경별 값은
`application-{profile}.properties` 로 분리했다. 기본 활성 프로파일은 `local`.

| 파일 | 용도 | 특징 |
|---|---|---|
| `application.properties` | 공통 | `spring.profiles.active=local` (기본값), `ddl-auto=none` 등 환경 무관 설정 |
| `application-local.properties` | 로컬 개발 | DB 접속 정보에 `localhost`/`matzzang` 기본값이 있어 별도 설정 없이 바로 동작. `show-sql=true`, 앱 패키지 로그 `DEBUG` |
| `application-prod.properties` | 배포 | DB 접속 정보에 기본값이 없어 `DB_HOST` 등 환경변수를 빠뜨리면 기동 시점에 즉시 실패(fail-fast). `show-sql=false`, 로그 레벨 `INFO`/`WARN` |

- 프로파일 전환은 `SPRING_PROFILES_ACTIVE` 환경변수로 한다 (Spring Boot가
  자동으로 `spring.profiles.active` 에 매핑).
- `docker-compose.yml` 의 `app` 서비스는 로컬 개발용 스택이므로
  `SPRING_PROFILES_ACTIVE=local` 을 기본으로 넘긴다.
- 실제 배포 환경(서버, CI/CD 등)에서는 `SPRING_PROFILES_ACTIVE=prod` 와 함께
  `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` 를 반드시
  주입해야 한다. 아직 별도의 배포용 compose 파일이나 CI/CD 파이프라인은
  구성되어 있지 않고, 이번 작업 범위는 프로파일 분리까지다.

## DB 스키마 관리

스키마는 Flyway 마이그레이션(`src/main/resources/schema/V*__*.sql`)으로 관리하며,
앱이 기동할 때 자동으로 적용된다. 그래서 `spring.jpa.hibernate.ddl-auto` 는 `none` 이다.
`postgres` 컨테이너는 빈 DB만 제공하고 테이블은 만들지 않는다.

스키마를 바꿔야 하면:
1. 새 버전 번호의 마이그레이션 파일을 추가한다 (기존 파일은 수정하지 않는다)
2. 앱 이미지를 다시 빌드한다 — SQL 파일이 jar 안에 포함되기 때문

```bash
docker compose up -d --build app
```

마이그레이션 이력이 꼬였거나 DB를 처음부터 다시 만들고 싶으면 볼륨을 지운다.

```bash
docker compose down -v   # postgres_data 볼륨 삭제 (DB 데이터 삭제)
docker compose up -d --build
```

## 향후 계획: Kafka / Redis

`docker-compose.yml` 하단에 `redis`, `kafka` 서비스 정의를 주석 처리된 형태로
남겨 두었다. 실제로 필요해지면:

1. 주석을 해제하고 이미지/버전을 확정
2. 앱이 사용할 접속 정보(`REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS` 등)를
   `.env` 와 프로파일별 `application-{profile}.properties` 에 같은 패턴(local은
   `${VAR:-기본값}`, prod는 `${VAR}`)으로 추가
3. `app` 서비스의 `depends_on` 에 추가해서 기동 순서를 보장
4. 필요한 클라이언트 의존성(`spring-boot-starter-data-redis`,
   `spring-kafka` 등)을 `build.gradle` 에 추가

## 검증 내역

아래는 이 문서를 작성하며 실제로 확인한 내용이다.

- `docker compose build` — `postgres`, `app` 이미지 모두 정상 빌드
- `docker compose up -d` — `postgres` healthy 이후 `app` 정상 기동, 로그에서
  HikariCP가 `postgres:5432` 에 연결 성공한 것 확인
- `docker compose down` 후 `docker compose up` 재기동 시 `postgres_data` 볼륨에
  데이터가 유지되는 것 확인
- 기본(`SPRING_PROFILES_ACTIVE=local`)으로 기동 시 로그에 `The following 1
  profile is active: "local"` 출력, API 정상 응답 확인
- `SPRING_PROFILES_ACTIVE=prod` 로 `DB_HOST` 등을 비운 채 기동 시, DB 접속 정보
  없이 Hibernate가 Dialect를 결정하지 못해 기동 시점에 즉시 예외로 실패하는
  것 확인 (의도한 fail-fast 동작)
