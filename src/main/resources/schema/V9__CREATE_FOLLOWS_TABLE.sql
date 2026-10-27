CREATE TABLE follows (
    id            BIGSERIAL PRIMARY KEY,
    follower_id   BIGINT    NOT NULL REFERENCES users (id),
    following_id  BIGINT    NOT NULL REFERENCES users (id),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_follows_follower_following UNIQUE (follower_id, following_id)
);

CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);
