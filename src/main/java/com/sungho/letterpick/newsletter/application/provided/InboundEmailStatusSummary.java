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
        if (receivedFrom.isAfter(receivedTo)) {
            throw new IllegalArgumentException("receivedFrom must be before or equal to receivedTo");
        }
        if (totalCount < 0) {
            throw new IllegalArgumentException("totalCount must be non-negative");
        }
        statusCounts = List.copyOf(requireNonNull(statusCounts));
        statusCounts.forEach(statusCount -> {
            requireNonNull(statusCount);
            requireNonNull(statusCount.count(), "status count must not be null");
            if (statusCount.count() < 0) {
                throw new IllegalArgumentException("status count must be non-negative");
            }
        });
    }
}
