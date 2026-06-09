package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.domain.PublicIssueViewCount;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicIssueViewCountRepository extends JpaRepository<PublicIssueViewCount, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO public_issue_view_count (issue_id, view_count, updated_at)
            VALUES (:issueId, :viewCount, :updatedAt)
            ON DUPLICATE KEY UPDATE
                updated_at = IF(VALUES(view_count) > view_count, VALUES(updated_at), updated_at),
                view_count = GREATEST(view_count, VALUES(view_count))
            """, nativeQuery = true)
    void upsertSnapshot(@Param("issueId") Long issueId,
                        @Param("viewCount") long viewCount,
                        @Param("updatedAt") Instant updatedAt);
}
