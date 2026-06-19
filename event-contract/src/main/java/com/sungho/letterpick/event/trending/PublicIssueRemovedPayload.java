package com.sungho.letterpick.event.trending;

import java.time.Instant;
import java.util.Objects;

public record PublicIssueRemovedPayload(
        Long issueId,
        Instant publicFeedCollectedAt
) {

    public PublicIssueRemovedPayload {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");
    }
}
