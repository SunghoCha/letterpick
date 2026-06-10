package com.sungho.letterpick.trending.viewcount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "public_issue_view_count_snapshot")
public class PublicIssueViewCountSnapshot {

    @Id
    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "snapshot_occurred_at", nullable = false)
    private Instant snapshotOccurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private PublicIssueViewCountSnapshot(Long issueId, long viewCount, Instant snapshotOccurredAt, Instant now) {
        this.issueId = requireNonNull(issueId, "issueId must not be null");
        this.viewCount = viewCount;
        this.snapshotOccurredAt = requireNonNull(snapshotOccurredAt, "snapshotOccurredAt must not be null");
        this.createdAt = requireNonNull(now, "now must not be null");
        this.updatedAt = now;

        if (viewCount < 0) {
            throw new IllegalArgumentException("viewCount must not be negative");
        }
    }

    public static PublicIssueViewCountSnapshot record(Long issueId, long viewCount,
                                                      Instant snapshotOccurredAt, Instant now) {
        return new PublicIssueViewCountSnapshot(issueId, viewCount, snapshotOccurredAt, now);
    }
}
