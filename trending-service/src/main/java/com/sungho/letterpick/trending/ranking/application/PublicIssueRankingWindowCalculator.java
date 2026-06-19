package com.sungho.letterpick.trending.ranking.application;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
public class PublicIssueRankingWindowCalculator {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");

    public List<PublicIssueRankingWindow> dailyAndWeeklyWindowsFor(Instant publicFeedCollectedAt) {
        requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");

        LocalDate collectedDate = LocalDate.ofInstant(publicFeedCollectedAt, RANKING_ZONE);
        return List.of(
                PublicIssueRankingWindow.daily(collectedDate, RANKING_ZONE),
                PublicIssueRankingWindow.weekly(collectedDate, RANKING_ZONE)
        );
    }
}
