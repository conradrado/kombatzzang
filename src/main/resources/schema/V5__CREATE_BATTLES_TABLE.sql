CREATE TABLE battles (
    id             BIGSERIAL PRIMARY KEY,
    challenger_id  BIGINT       NOT NULL REFERENCES users (id),
    opponent_id    BIGINT       NOT NULL REFERENCES users (id),
    sport          VARCHAR(30)  NOT NULL,
    scheduled_at   TIMESTAMP,
    location       VARCHAR(255),
    message        TEXT,
    status         VARCHAR(20)  NOT NULL,
    winner_id      BIGINT       REFERENCES users (id),
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP
);

CREATE INDEX idx_battles_challenger_id ON battles (challenger_id);
CREATE INDEX idx_battles_opponent_id ON battles (opponent_id);
