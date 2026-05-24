package com.sungho.letterpick.newsletter.application.provided;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;

public record InboundEmailStatusSummary(
        Instant receivedFrom,
        Instant receivedTo,
        long totalCount,
        List<InboundEmailStatusCount> statusCounts
) {

    public InboundEmailStatusSummary {
        requireNonNull(receivedFrom);
        requireNonNull(receivedTo);
        statusCounts = List.copyOf(requireNonNull(statusCounts));
    }
}
