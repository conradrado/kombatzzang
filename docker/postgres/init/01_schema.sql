-- Generated from src/main/resources/schema/V1__INITIAL_SCHEAM.sql (DBML)
-- Runs once, on first container start, via /docker-entrypoint-initdb.d/

CREATE TYPE fight_rule AS ENUM ('MMA', 'BOXING', 'MUAY_THAI');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE follows (
    following_user_id BIGINT    NOT NULL REFERENCES users (id),
    followed_user_id  BIGINT    NOT NULL REFERENCES users (id),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (following_user_id, followed_user_id)
);

CREATE TABLE user_stat (
    user_id      BIGINT PRIMARY KEY REFERENCES users (id),
    win          INTEGER NOT NULL DEFAULT 0,
    lose         INTEGER NOT NULL DEFAULT 0,
    rank         INTEGER,
    weight_class VARCHAR(50)
);

CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    user_id    BIGINT NOT NULL REFERENCES users (id),
    status     VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE fights (
    id           BIGSERIAL PRIMARY KEY,
    fighter_1_id BIGINT REFERENCES users (id),
    fighter_2_id BIGINT REFERENCES users (id),
    referee_id   BIGINT REFERENCES users (id),
    winner_id    BIGINT REFERENCES users (id),
    weight_class VARCHAR(50),
    status       VARCHAR(20),
    rounds       INTEGER,
    rule         fight_rule,
    fight_at     TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
