package com.sungho.letterpick.trending.ranking.application.provided;

public final class PublicIssueRankingLimit {

    public static final String DEFAULT_VALUE = "20";
    public static final int MIN = 1;
    public static final int MAX = 100;

    private PublicIssueRankingLimit() {
    }

    public static boolean isInRange(int limit) {
        return limit >= MIN && limit <= MAX;
    }

    public static void validate(int limit) {
        if (!isInRange(limit)) {
            throw new IllegalArgumentException(errorMessage());
        }
    }

    public static String errorMessage() {
        return "limit must be between " + MIN + " and " + MAX;
    }
}
