package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateReader;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PublicIssueRankingScoreCalculator {

    public int calculate(RedisPublicIssueRankingStateReader.AvailableIssueRankingState state) {
        Objects.requireNonNull(state, "state must not be null");

        return Math.toIntExact(state.viewCount());
    }
}
