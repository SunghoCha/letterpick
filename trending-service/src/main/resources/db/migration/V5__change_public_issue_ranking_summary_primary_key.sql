ALTER TABLE public_issue_ranking_summary
    DROP PRIMARY KEY,
    DROP COLUMN id,
    DROP INDEX uk_public_issue_ranking_summary_window_issue,
    ADD PRIMARY KEY (window_type, window_key, issue_id);
