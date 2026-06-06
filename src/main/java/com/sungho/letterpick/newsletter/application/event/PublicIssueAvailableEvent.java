package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.newsletter.domain.NewsletterCategory;

import java.time.Instant;
import java.util.Objects;

public record PublicIssueAvailableEvent(
        String eventId,
        Long issueId,
        Long newsletterId,
        NewsletterCategory category,
        Instant publicFeedCollectedAt
) {

    public PublicIssueAvailableEvent {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(newsletterId, "newsletterId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");
    }
}
