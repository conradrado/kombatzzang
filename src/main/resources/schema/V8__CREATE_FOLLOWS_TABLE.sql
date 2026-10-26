CREATE TABLE follows (
    id           BIGSERIAL PRIMARY KEY,
    follower_id  BIGINT     NOT NULL REFERENCES users (id),
    followee_id  BIGINT     NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP  NOT NULL DEFAULT now(),
    CONSTRAINT uk_follows_follower_followee UNIQUE (follower_id, followee_id)
);

CREATE INDEX idx_follows_followee_id ON follows (followee_id);
