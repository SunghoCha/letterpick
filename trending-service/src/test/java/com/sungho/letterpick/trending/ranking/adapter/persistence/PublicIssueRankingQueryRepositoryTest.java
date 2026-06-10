package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.viewcount.PublicIssueViewCountSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TrendingServiceTestConfiguration.class)
@ActiveProfiles("test")
class PublicIssueRankingQueryRepositoryTest {

    @Autowired
    private PublicIssueRankingQueryRepository rankingQueryRepository;

    @Autowired
    private PublicIssueCandidateRepository candidateRepository;

    @Autowired
    private PublicIssueViewCountSnapshotRepository viewCountSnapshotRepository;

    @Test
    @DisplayName("AVAILABLE 후보와 조회수 snapshot을 조합해 오늘 인기 이슈를 조회수 순으로 조회한다")
    void find_top_ranking_items_by_window() {
        // given
        Instant windowStart = Instant.parse("2050-06-09T15:00:00Z");
        Instant windowEnd = Instant.parse("2050-06-10T15:00:00Z");

        saveAvailable(10L, Instant.parse("2050-06-10T00:00:00Z"));
        saveSnapshot(10L, 120L);
        saveAvailable(21L, Instant.parse("2050-06-10T01:00:00Z"));
        saveSnapshot(21L, 80L);
        saveAvailable(60L, Instant.parse("2050-06-10T02:00:00Z"));
        saveSnapshot(60L, 120L);

        saveAvailable(30L, Instant.parse("2050-06-09T14:59:59Z"));
        saveSnapshot(30L, 500L);
        candidateRepository.upsertRemoved(
                40L,
                Instant.parse("2050-06-10T03:00:00Z"),
                Instant.parse("2050-06-10T03:00:00Z")
        );
        saveSnapshot(40L, 999L);
        saveAvailable(50L, Instant.parse("2050-06-10T04:00:00Z"));

        // when
        List<PublicIssueRankingItem> rankingItems = rankingQueryRepository.findTopByWindow(
                PublicIssueCandidateStatus.AVAILABLE,
                windowStart,
                windowEnd,
                PageRequest.of(0, 3)
        );

        // then
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::issueId)
                .containsExactly(60L, 10L, 21L);
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::score)
                .containsExactly(120L, 120L, 80L);
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::viewCount)
                .containsExactly(120L, 120L, 80L);
    }

    private void saveAvailable(Long issueId, Instant publicFeedCollectedAt) {
        candidateRepository.insertAvailableIfAbsent(
                issueId,
                issueId + 1_000L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                publicFeedCollectedAt,
                publicFeedCollectedAt,
                publicFeedCollectedAt
        );
    }

    private void saveSnapshot(Long issueId, long viewCount) {
        viewCountSnapshotRepository.upsertSnapshot(
                issueId,
                viewCount,
                Instant.parse("2050-06-10T00:00:00Z"),
                Instant.parse("2050-06-10T00:00:00Z")
        );
    }
}
