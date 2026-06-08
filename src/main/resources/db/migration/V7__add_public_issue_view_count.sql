CREATE TABLE public_issue_view_count (
    issue_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_public_issue_view_count_issue
        FOREIGN KEY (issue_id) REFERENCES newsletter_issue (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
