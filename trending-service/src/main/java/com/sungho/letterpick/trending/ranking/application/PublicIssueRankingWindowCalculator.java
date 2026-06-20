package com.sungho.letterpick.trending.ranking.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
public class PublicIssueRankingWindowCalculator {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public PublicIssueRankingWindowCalculator(Clock clock) {
        this.clock = requireNonNull(clock, "clock must not be null");
    }

    public PublicIssueRankingWindow currentWindow(PublicIssueRankingWindowType windowType) {
        requireNonNull(windowType, "windowType must not be null");

        LocalDate today = LocalDate.now(clock.withZone(RANKING_ZONE));
        return switch (windowType) {
            case DAILY -> PublicIssueRankingWindow.daily(today, RANKING_ZONE);
            case WEEKLY -> PublicIssueRankingWindow.weekly(today, RANKING_ZONE);
        };
    }

    public List<PublicIssueRankingWindow> dailyAndWeeklyWindowsFor(Instant publicFeedCollectedAt) {
        requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");

        LocalDate collectedDate = LocalDate.ofInstant(publicFeedCollectedAt, RANKING_ZONE);
        return List.of(
                PublicIssueRankingWindow.daily(collectedDate, RANKING_ZONE),
                PublicIssueRankingWindow.weekly(collectedDate, RANKING_ZONE)
        );
    }
}
