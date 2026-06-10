package com.sungho.letterpick.trending.viewcount;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PublicIssueViewCountSnapshotRepository
        extends JpaRepository<PublicIssueViewCountSnapshot, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO public_issue_view_count_snapshot (
                issue_id,
                view_count,
                snapshot_occurred_at,
                created_at,
                updated_at
            ) VALUES (
                :issueId,
                :viewCount,
                :snapshotOccurredAt,
                :now,
                :now
            ) AS new(
                new_issue_id,
                new_view_count,
                new_snapshot_occurred_at,
                new_created_at,
                new_updated_at
            )
            ON DUPLICATE KEY UPDATE
                snapshot_occurred_at = IF(new_view_count > view_count, new_snapshot_occurred_at, snapshot_occurred_at),
                updated_at = IF(new_view_count > view_count, new_updated_at, updated_at),
                view_count = GREATEST(view_count, new_view_count)
            """, nativeQuery = true)
    void upsertSnapshot(@Param("issueId") Long issueId,
                        @Param("viewCount") long viewCount,
                        @Param("snapshotOccurredAt") Instant snapshotOccurredAt,
                        @Param("now") Instant now);
}
