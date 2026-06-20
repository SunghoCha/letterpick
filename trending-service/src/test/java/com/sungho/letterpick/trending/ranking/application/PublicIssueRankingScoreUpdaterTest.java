package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateReader;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicIssueRankingScoreUpdaterTest {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-10T01:00:00Z"), ZoneOffset.UTC);
    private static final Instant COLLECTED_AT = Instant.parse("2050-06-10T00:59:00Z");
    private static final Instant CALCULATED_AT = Instant.parse("2050-06-10T01:00:00Z");

    @Mock
    private RedisPublicIssueRankingStateReader rankingStateReader;

    @Mock
    private PublicIssueRankingScoreCalculator scoreCalculator;

    @Mock
    private PublicIssueRankingSummaryWriter rankingSummaryWriter;

    private PublicIssueRankingScoreUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new PublicIssueRankingScoreUpdater(
                rankingStateReader,
                scoreCalculator,
                rankingSummaryWriter,
                new PublicIssueRankingWindowCalculator(CLOCK)
        );
    }

    @Test
    @DisplayName("score factor로 계산한 점수를 daily/weekly ranking에 반영한다")
    void update_daily_and_weekly_ranking_score() {
        // given
        var state = new RedisPublicIssueRankingStateReader.AvailableIssueRankingState(
                COLLECTED_AT,
                150L
        );
        when(rankingStateReader.findAvailableIssueState(1L))
                .thenReturn(Optional.of(state));
        when(scoreCalculator.calculate(state))
                .thenReturn(300L);

        // when
        updater.refresh(1L, CALCULATED_AT);

        // then
        verify(scoreCalculator).calculate(state);
        verify(rankingSummaryWriter).save(dailyWindow(), 1L, 300L, CALCULATED_AT);
        verify(rankingSummaryWriter).save(weeklyWindow(), 1L, 300L, CALCULATED_AT);
        verifyNoMoreInteractions(rankingSummaryWriter);
    }

    @Test
    @DisplayName("공개 ranking state가 없으면 ranking을 갱신하지 않는다")
    void skip_when_available_ranking_state_does_not_exist() {
        // given
        when(rankingStateReader.findAvailableIssueState(1L))
                .thenReturn(Optional.empty());

        // when
        updater.refresh(1L, CALCULATED_AT);

        // then
        verifyNoInteractions(rankingSummaryWriter);
    }

    private PublicIssueRankingWindow dailyWindow() {
        return PublicIssueRankingWindow.daily(LocalDate.of(2050, 6, 10), RANKING_ZONE);
    }

    private PublicIssueRankingWindow weeklyWindow() {
        return PublicIssueRankingWindow.weekly(LocalDate.of(2050, 6, 10), RANKING_ZONE);
    }
}
