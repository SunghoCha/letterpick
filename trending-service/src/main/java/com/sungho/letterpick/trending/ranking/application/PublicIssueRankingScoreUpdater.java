package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateReader;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class PublicIssueRankingScoreUpdater {

    private final RedisPublicIssueRankingStateReader rankingStateReader;
    private final PublicIssueRankingScoreCalculator scoreCalculator;
    private final PublicIssueRankingSummaryWriter rankingSummaryWriter;
    private final PublicIssueRankingWindowCalculator windowCalculator;

    public PublicIssueRankingScoreUpdater(RedisPublicIssueRankingStateReader rankingStateReader,
                                          PublicIssueRankingScoreCalculator scoreCalculator,
                                          PublicIssueRankingSummaryWriter rankingSummaryWriter,
                                          PublicIssueRankingWindowCalculator windowCalculator) {
        this.rankingStateReader = Objects.requireNonNull(rankingStateReader, "rankingStateReader must not be null");
        this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator must not be null");
        this.rankingSummaryWriter = Objects.requireNonNull(rankingSummaryWriter, "rankingSummaryWriter must not be null");
        this.windowCalculator = Objects.requireNonNull(windowCalculator, "windowCalculator must not be null");
    }

    public void refresh(Long issueId, Instant calculatedAt) {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");

        rankingStateReader.findAvailableIssueState(issueId)
                .ifPresent(state -> update(issueId, calculatedAt, state));
    }

    private void update(Long issueId,
                        Instant calculatedAt,
                        RedisPublicIssueRankingStateReader.AvailableIssueRankingState state) {
        long score = scoreCalculator.calculate(state);

        for (PublicIssueRankingWindow window : windowCalculator.dailyAndWeeklyWindowsFor(state.publicFeedCollectedAt())) {
            rankingSummaryWriter.save(window, issueId, score, calculatedAt);
        }
    }
}
