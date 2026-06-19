package com.sungho.letterpick.trending.ranking.application;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static java.util.Objects.requireNonNull;

public record PublicIssueRankingWindow(
        PublicIssueRankingWindowType type,
        String key,
        Instant startInclusive,
        Instant endExclusive
) {

    public PublicIssueRankingWindow {
        requireNonNull(type, "type must not be null");
        requireNonNull(key, "key must not be null");
        requireNonNull(startInclusive, "startInclusive must not be null");
        requireNonNull(endExclusive, "endExclusive must not be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
    }

    public static PublicIssueRankingWindow daily(LocalDate date, ZoneId zone) {
        requireNonNull(date, "date must not be null");
        requireNonNull(zone, "zone must not be null");

        return fromDateRange(PublicIssueRankingWindowType.DAILY, date, date.plusDays(1), zone);
    }

    public static PublicIssueRankingWindow weekly(LocalDate date, ZoneId zone) {
        requireNonNull(date, "date must not be null");
        requireNonNull(zone, "zone must not be null");

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return fromDateRange(PublicIssueRankingWindowType.WEEKLY, weekStart, weekStart.plusWeeks(1), zone);
    }

    private static PublicIssueRankingWindow fromDateRange(PublicIssueRankingWindowType type,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          ZoneId zone) {
        Instant startInclusive = startDate.atStartOfDay(zone).toInstant();
        Instant endExclusive = endDate.atStartOfDay(zone).toInstant();
        return new PublicIssueRankingWindow(
                type,
                startDate.toString(),
                startInclusive,
                endExclusive
        );
    }
}
