package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PublicIssueRankingScoreCalculatorTest {

    private final PublicIssueRankingScoreCalculator calculator = new PublicIssueRankingScoreCalculator();

    @Test
    @DisplayName("현재 ranking score는 조회수 기준으로 계산한다")
    void calculate_score_from_view_count() {
        // given
        var state = new RedisPublicIssueRankingStateReader.AvailableIssueRankingState(
                Instant.parse("2050-06-10T00:59:00Z"),
                150L
        );

        // when
        int score = calculator.calculate(state);

        // then
        assertThat(score).isEqualTo(150);
    }
}
