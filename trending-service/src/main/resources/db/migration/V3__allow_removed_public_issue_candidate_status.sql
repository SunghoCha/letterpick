ALTER TABLE public_issue_candidate
    MODIFY newsletter_id BIGINT NULL,
    MODIFY category VARCHAR(50) NULL,
    MODIFY public_feed_collected_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_public_issue_candidate_available_metadata
        CHECK (
            status <> 'AVAILABLE'
            OR (
                newsletter_id IS NOT NULL
                AND category IS NOT NULL
                AND public_feed_collected_at IS NOT NULL
            )
        );
