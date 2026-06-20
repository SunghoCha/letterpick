package com.sungho.letterpick.newsletter.application.provided;

import static java.util.Objects.requireNonNull;

public record PublicIssueRankingItem(
        Long issueId,
        long score
) {

    public PublicIssueRankingItem {
        requireNonNull(issueId, "issueId must not be null");
        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
    }
}
