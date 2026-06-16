package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowType;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TrendingServiceTestConfiguration.class)
@ActiveProfiles("test")
class PublicIssueRankingSummaryRepositoryTest {

    @Autowired
    private PublicIssueRankingSummaryRepository rankingSummaryRepository;

    @Autowired
    private PublicIssueCandidateRepository candidateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("summary row는 window와 issue 조합을 primary key로 식별한다")
    void primary_key_is_summary_window_issue() {
        // when
        List<String> primaryKeyColumns = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'public_issue_ranking_summary'
                  AND CONSTRAINT_NAME = 'PRIMARY'
                ORDER BY ORDINAL_POSITION
                """, String.class);

        // then
        assertThat(primaryKeyColumns)
                .containsExactly("window_type", "window_key", "issue_id");
    }

    @Test
    @DisplayName("AVAILABLE summary window의 인기 이슈를 score 순으로 조회한다")
    void find_top_by_summary_window() {
        // given
        saveAvailable(10L);
        saveAvailable(40L);
        saveAvailable(60L);
        saveRemoved(90L);
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 10L, 120L);
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 40L, 999L);
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 60L, 999L);
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 90L, 2_000L);
        saveSummary(PublicIssueRankingWindowType.WEEKLY, "2050-06-06", 20L, 1_000L);

        // when
        List<PublicIssueRankingItem> rankingItems = rankingSummaryRepository.findTopByWindow(
                PublicIssueRankingWindowType.DAILY.name(),
                "2050-06-10",
                PublicIssueCandidateStatus.AVAILABLE,
                PageRequest.of(0, 3)
        );

        // then
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::issueId)
                .containsExactly(60L, 40L, 10L);
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::score)
                .containsExactly(999L, 999L, 120L);
    }

    @Test
    @DisplayName("issueId 기준으로 모든 window의 summary를 삭제한다")
    void delete_summary_by_issue_id() {
        // given
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 10L, 120L);
        saveSummary(PublicIssueRankingWindowType.WEEKLY, "2050-06-06", 10L, 120L);
        saveSummary(PublicIssueRankingWindowType.MONTHLY, "2050-06-01", 10L, 120L);
        saveSummary(PublicIssueRankingWindowType.DAILY, "2050-06-10", 20L, 80L);

        // when
        rankingSummaryRepository.deleteByIssueId(10L);

        // then
        assertThat(rankingSummaryRepository.findAll())
                .extracting(PublicIssueRankingSummary::getIssueId)
                .containsExactly(20L);
    }

    private void saveAvailable(Long issueId) {
        Instant now = Instant.parse("2050-06-10T01:00:00Z");
        candidateRepository.insertAvailableIfAbsent(
                issueId,
                issueId + 1_000L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                now,
                now,
                now
        );
    }

    private void saveRemoved(Long issueId) {
        Instant now = Instant.parse("2050-06-10T01:00:00Z");
        candidateRepository.upsertRemoved(
                issueId,
                now,
                now
        );
    }

    private void saveSummary(PublicIssueRankingWindowType windowType, String windowKey,
                             Long issueId, long score) {
        rankingSummaryRepository.upsertSummary(
                windowType.name(),
                windowKey,
                issueId,
                score,
                Instant.parse("2050-06-10T01:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );
    }
}
