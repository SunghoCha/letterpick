package com.sungho.letterpick.event.trending;

import java.util.Objects;

public record PublicIssueRemovedPayload(
        Long issueId
) {

    public PublicIssueRemovedPayload {
        Objects.requireNonNull(issueId, "issueId must not be null");
    }
}
