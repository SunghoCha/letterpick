ALTER TABLE member
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

UPDATE member
SET created_at = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE member
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE newsletter
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

UPDATE newsletter
SET created_at = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE newsletter
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE member_newsletter
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

UPDATE member_newsletter
SET created_at = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE member_newsletter
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE inbound_email
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

UPDATE inbound_email
SET created_at = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE inbound_email
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;

ALTER TABLE newsletter_issue
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

UPDATE newsletter_issue
SET created_at = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE newsletter_issue
    MODIFY created_at DATETIME(6) NOT NULL,
    MODIFY updated_at DATETIME(6) NOT NULL;
