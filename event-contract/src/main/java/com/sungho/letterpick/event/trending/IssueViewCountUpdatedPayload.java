package com.sungho.letterpick.event.trending;

import java.util.Objects;

public record IssueViewCountUpdatedPayload(
        Long issueId,
        Long viewCount
) {

    public IssueViewCountUpdatedPayload {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(viewCount, "viewCount must not be null");

        if (viewCount < 0) {
            throw new IllegalArgumentException("viewCount must not be negative");
        }
    }
}
