CREATE TABLE public_issue_ranking_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    window_type VARCHAR(20) NOT NULL,
    window_key VARCHAR(20) NOT NULL,
    issue_id BIGINT NOT NULL,
    score BIGINT NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_public_issue_ranking_summary_window_issue (window_type, window_key, issue_id),
    INDEX idx_public_issue_ranking_summary_rank (window_type, window_key, score DESC, issue_id DESC),
    INDEX idx_public_issue_ranking_summary_issue_id (issue_id),
    CONSTRAINT chk_public_issue_ranking_summary_score_non_negative CHECK (score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
