CREATE TABLE users (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(20)  NOT NULL,
    email              VARCHAR(255) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    profile_image_key  VARCHAR(255),
    role               VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- follows / userStat / posts / fights 등 나머지 테이블은
-- 해당 도메인(팔로우, 전적, 게시글, 대전)이 실제로 구현될 때 별도 마이그레이션으로 추가한다.
