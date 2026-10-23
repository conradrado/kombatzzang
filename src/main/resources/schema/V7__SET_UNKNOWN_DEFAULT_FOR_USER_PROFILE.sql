UPDATE users SET region = 'UNKNOWN' WHERE region IS NULL;
UPDATE users SET sport = 'UNKNOWN' WHERE sport IS NULL;
UPDATE users SET weight_class = 'UNKNOWN' WHERE weight_class IS NULL;
UPDATE users SET activity_region = 'UNKNOWN' WHERE activity_region IS NULL;

ALTER TABLE users
    ALTER COLUMN region SET DEFAULT 'UNKNOWN',
    ALTER COLUMN sport SET DEFAULT 'UNKNOWN',
    ALTER COLUMN weight_class SET DEFAULT 'UNKNOWN',
    ALTER COLUMN activity_region SET DEFAULT 'UNKNOWN';
