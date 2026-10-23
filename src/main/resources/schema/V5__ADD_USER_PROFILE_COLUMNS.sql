ALTER TABLE users
    ADD COLUMN sport           VARCHAR(30)  NULL,
    ADD COLUMN weight_class    VARCHAR(50)  NULL,
    ADD COLUMN activity_region VARCHAR(100) NULL,
    ADD COLUMN wins            INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN losses          INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN level           VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    ADD COLUMN bio             VARCHAR(500) NULL;

CREATE INDEX idx_users_activity_region ON users (activity_region);
