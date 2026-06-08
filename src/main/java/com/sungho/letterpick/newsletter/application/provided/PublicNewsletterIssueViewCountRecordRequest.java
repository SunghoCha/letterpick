package com.sungho.letterpick.newsletter.application.provided;

import java.util.Objects;

public record PublicNewsletterIssueViewCountRecordRequest(
        Long issueId,
        String actorKey
) {

    public PublicNewsletterIssueViewCountRecordRequest {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(actorKey, "actorKey must not be null");

        if (actorKey.isBlank()) {
            throw new IllegalArgumentException("actorKey must not be blank");
        }
    }
}
