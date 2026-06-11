package com.sungho.letterpick.trending.ranking.application.provided;

import static java.util.Objects.requireNonNull;

public record PublicIssueRankingItem(
        Long issueId,
        long score,
        long viewCount
) {

    public PublicIssueRankingItem {
        requireNonNull(issueId, "issueId must not be null");
        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
        if (viewCount < 0) {
            throw new IllegalArgumentException("viewCount must not be negative");
        }
    }
}
