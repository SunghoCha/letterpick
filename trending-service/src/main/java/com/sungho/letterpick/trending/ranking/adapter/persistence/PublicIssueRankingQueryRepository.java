package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PublicIssueRankingQueryRepository extends Repository<PublicIssueCandidate, Long> {

    @Query("""
            SELECT new com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem(
                c.issueId,
                v.viewCount,
                v.viewCount
            )
            FROM PublicIssueCandidate c
            JOIN PublicIssueViewCountSnapshot v ON v.issueId = c.issueId
            WHERE c.status = :status
              AND c.publicFeedCollectedAt >= :windowStart
              AND c.publicFeedCollectedAt < :windowEnd
            ORDER BY v.viewCount DESC, c.issueId DESC
            """)
    List<PublicIssueRankingItem> findTopByWindow(@Param("status") PublicIssueCandidateStatus status,
                                                 @Param("windowStart") Instant windowStart,
                                                 @Param("windowEnd") Instant windowEnd,
                                                 Pageable pageable);
}
