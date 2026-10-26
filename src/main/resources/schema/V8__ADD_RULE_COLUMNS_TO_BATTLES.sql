ALTER TABLE battles
    ADD COLUMN round_count   INT         NOT NULL DEFAULT 3,
    ADD COLUMN round_minutes INT         NOT NULL DEFAULT 3,
    ADD COLUMN weight_class  VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN';
