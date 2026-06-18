package com.sungho.letterpick.newsletter.application.event;

import java.time.Instant;
import java.util.Objects;

public record PublicIssueRemovedEvent(
        String eventId,
        Long issueId,
        Instant publicFeedCollectedAt,
        Instant occurredAt
) {

    public PublicIssueRemovedEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
