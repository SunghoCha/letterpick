package com.sungho.letterpick.trending.publicissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface PublicIssueCandidateRepository extends JpaRepository<PublicIssueCandidate, Long> {

    Optional<PublicIssueCandidate> findByIssueId(Long issueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM PublicIssueCandidate c
            WHERE c.issueId = :issueId
            """)
    Optional<PublicIssueCandidate> findByIssueIdForUpdate(@Param("issueId") Long issueId);

    @Modifying
    @Query(value = """
            INSERT INTO public_issue_candidate (
                issue_id,
                newsletter_id,
                category,
                status,
                public_feed_collected_at,
                created_at,
                updated_at
            ) VALUES (
                :issueId,
                :newsletterId,
                :category,
                :status,
                :publicFeedCollectedAt,
                :createdAt,
                :updatedAt
            )
            ON DUPLICATE KEY UPDATE issue_id = issue_id
            """, nativeQuery = true)
    void insertAvailableIfAbsent(@Param("issueId") Long issueId,
                                 @Param("newsletterId") Long newsletterId,
                                 @Param("category") String category,
                                 @Param("status") String status,
                                 @Param("publicFeedCollectedAt") Instant publicFeedCollectedAt,
                                 @Param("createdAt") Instant createdAt,
                                 @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query(value = """
            INSERT INTO public_issue_candidate (
                issue_id,
                status,
                created_at,
                updated_at
            ) VALUES (
                :issueId,
                'REMOVED',
                :createdAt,
                :updatedAt
            )
            ON DUPLICATE KEY UPDATE
                status = 'REMOVED',
                updated_at = :updatedAt
            """, nativeQuery = true)
    void upsertRemoved(@Param("issueId") Long issueId,
                       @Param("createdAt") Instant createdAt,
                       @Param("updatedAt") Instant updatedAt);
}
