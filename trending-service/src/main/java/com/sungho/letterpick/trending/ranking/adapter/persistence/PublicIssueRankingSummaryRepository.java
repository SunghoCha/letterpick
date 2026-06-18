package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PublicIssueRankingSummaryRepository
        extends JpaRepository<PublicIssueRankingSummary, PublicIssueRankingSummaryId> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO public_issue_ranking_summary (
                window_type,
                window_key,
                issue_id,
                score,
                calculated_at,
                created_at,
                updated_at
            ) VALUES (
                :windowType,
                :windowKey,
                :issueId,
                :score,
                :calculatedAt,
                :now,
                :now
            ) AS new(
                new_window_type,
                new_window_key,
                new_issue_id,
                new_score,
                new_calculated_at,
                new_created_at,
                new_updated_at
            )
            ON DUPLICATE KEY UPDATE
                score = new_score,
                calculated_at = new_calculated_at,
                updated_at = new_updated_at
            """, nativeQuery = true)
    void upsertSummary(@Param("windowType") String windowType,
                       @Param("windowKey") String windowKey,
                       @Param("issueId") Long issueId,
                       @Param("score") long score,
                       @Param("calculatedAt") Instant calculatedAt,
                       @Param("now") Instant now);

    @Modifying
    @Query("""
            DELETE FROM PublicIssueRankingSummary s
            WHERE s.windowType = :windowType
              AND s.windowKey = :windowKey
              AND s.issueId = :issueId
            """)
    void deleteByWindowAndIssueId(@Param("windowType") String windowType,
                                  @Param("windowKey") String windowKey,
                                  @Param("issueId") Long issueId);

    @Query("""
            SELECT new com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem(
                s.issueId,
                s.score
            )
            FROM PublicIssueRankingSummary s
            JOIN PublicIssueCandidate c ON c.issueId = s.issueId
            WHERE s.windowType = :windowType
              AND s.windowKey = :windowKey
              AND c.status = :status
            ORDER BY s.score DESC, s.issueId DESC
            """)
    List<PublicIssueRankingItem> findTopByWindow(@Param("windowType") String windowType,
                                                 @Param("windowKey") String windowKey,
                                                 @Param("status") PublicIssueCandidateStatus status,
                                                 Pageable pageable);
}
