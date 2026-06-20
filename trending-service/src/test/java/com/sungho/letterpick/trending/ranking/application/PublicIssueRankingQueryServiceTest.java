package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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
                new PublicIssueRankingLimitPolicy(20, 100),
                new PublicIssueRankingWindowCalculator(CLOCK)
        );
    }

    @Test
    @DisplayName("DAILY는 Asia/Seoul 기준 오늘 범위로 인기 이슈를 조회한다")
    void find_daily_top_ranking_items() {
        // when
        rankingQueryService.findTop(PublicIssueRankingWindowType.DAILY, 20);

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
    @DisplayName("WEEKLY는 Asia/Seoul 기준 현재 주간 범위로 인기 이슈를 조회한다")
    void find_weekly_top_ranking_items() {
        // when
        rankingQueryService.findTop(PublicIssueRankingWindowType.WEEKLY, 20);

        // then
        verify(rankingReader).findTop(
                new PublicIssueRankingWindow(
                        PublicIssueRankingWindowType.WEEKLY,
                        "2050-06-06",
                        Instant.parse("2050-06-05T15:00:00Z"),
                        Instant.parse("2050-06-12T15:00:00Z")
                ),
                20
        );
    }

    @Test
    @DisplayName("limit이 없으면 기본 개수로 인기 이슈를 조회한다")
    void find_top_with_default_limit() {
        // when
        rankingQueryService.findTop(PublicIssueRankingWindowType.DAILY, null);

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
    @DisplayName("limit이 1보다 작으면 최소 1개 기준으로 인기 이슈를 조회한다")
    void clamp_non_positive_limit_to_minimum() {
        // when
        rankingQueryService.findTop(PublicIssueRankingWindowType.DAILY, 0);

        // then
        verify(rankingReader).findTop(
                new PublicIssueRankingWindow(
                        PublicIssueRankingWindowType.DAILY,
                        "2050-06-10",
                        Instant.parse("2050-06-09T15:00:00Z"),
                        Instant.parse("2050-06-10T15:00:00Z")
                ),
                1
        );
    }

    @Test
    @DisplayName("limit이 maxSize보다 크면 maxSize 기준으로 인기 이슈를 조회한다")
    void clamp_too_large_limit_to_max_size() {
        // when
        rankingQueryService.findTop(PublicIssueRankingWindowType.DAILY, 101);

        // then
        verify(rankingReader).findTop(
                new PublicIssueRankingWindow(
                        PublicIssueRankingWindowType.DAILY,
                        "2050-06-10",
                        Instant.parse("2050-06-09T15:00:00Z"),
                        Instant.parse("2050-06-10T15:00:00Z")
                ),
                100
        );
    }
}
