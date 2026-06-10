CREATE TABLE public_issue_view_count_snapshot (
    issue_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    snapshot_occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
