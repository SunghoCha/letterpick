package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RdsSummaryPublicIssueRankingSummaryWriterTest {

    @Mock
    private PublicIssueRankingSummaryRepository rankingSummaryRepository;

    private RdsSummaryPublicIssueRankingSummaryWriter writer;

    @BeforeEach
    void setUp() {
        writer = new RdsSummaryPublicIssueRankingSummaryWriter(rankingSummaryRepository);
    }

    @Test
    @DisplayName("window 정보로 RDS summary를 upsert한다")
    void save_summary_by_window() {
        // given
        PublicIssueRankingWindow window = PublicIssueRankingWindow.daily(
                LocalDate.of(2050, 6, 12),
                ZoneId.of("Asia/Seoul")
        );
        Instant calculatedAt = Instant.parse("2050-06-12T01:00:00Z");

        // when
        writer.save(window, 10L, 150L, calculatedAt);

        // then
        verify(rankingSummaryRepository).upsertSummary(
                "DAILY",
                "2050-06-12",
                10L,
                150L,
                calculatedAt,
                calculatedAt
        );
    }

    @Test
    @DisplayName("window 기준으로 RDS summary를 제거한다")
    void delete_summary_by_window() {
        // given
        PublicIssueRankingWindow window = PublicIssueRankingWindow.daily(
                LocalDate.of(2050, 6, 12),
                ZoneId.of("Asia/Seoul")
        );

        // when
        writer.delete(window, 10L);

        // then
        verify(rankingSummaryRepository).deleteByWindowAndIssueId(
                "DAILY",
                "2050-06-12",
                10L
        );
    }
}
