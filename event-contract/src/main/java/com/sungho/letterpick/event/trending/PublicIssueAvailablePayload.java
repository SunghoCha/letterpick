package com.sungho.letterpick.event.trending;

import java.time.Instant;
import java.util.Objects;

public record PublicIssueAvailablePayload(
        Long issueId,
        Long newsletterId,
        String category,
        Instant publicFeedCollectedAt
) {

    public PublicIssueAvailablePayload {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(newsletterId, "newsletterId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");
    }
}
