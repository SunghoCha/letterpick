package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicIssueRankingQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-10T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicIssueRankingReader rankingReader;

    private PublicIssueRankingQueryService rankingQueryService;

    @BeforeEach
    void setUp() {
        rankingQueryService = new PublicIssueRankingQueryService(
                rankingReader,
                CLOCK
        );
    }

    @Test
    @DisplayName("Asia/Seoul 기준 오늘 범위로 인기 이슈를 조회한다")
    void find_today_top_ranking_items() {
        // when
        rankingQueryService.findTodayTop(20);

        // then
        verify(rankingReader).findTop(
                new PublicIssueRankingWindow(
                        PublicIssueRankingWindowType.DAILY,
                        "2050-06-10",
                        Instant.parse("2050-06-09T15:00:00Z"),
                        Instant.parse("2050-06-10T15:00:00Z")
                ),
                20
        );
    }

    @Test
    @DisplayName("limit이 1보다 작으면 인기 이슈를 조회하지 않는다")
    void reject_non_positive_limit() {
        assertThatThrownBy(() -> rankingQueryService.findTodayTop(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between 1 and 100");
    }

    @Test
    @DisplayName("limit이 100보다 크면 인기 이슈를 조회하지 않는다")
    void reject_too_large_limit() {
        assertThatThrownBy(() -> rankingQueryService.findTodayTop(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between 1 and 100");
    }
}
