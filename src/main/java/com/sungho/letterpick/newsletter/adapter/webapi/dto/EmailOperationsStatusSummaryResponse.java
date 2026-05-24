package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import static java.util.Objects.requireNonNull;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import java.time.Instant;
import java.util.List;

public record EmailOperationsStatusSummaryResponse(
        Instant receivedFrom,
        Instant receivedTo,
        long totalCount,
        List<InboundEmailStatusCountResponse> statusCounts
) {

    public EmailOperationsStatusSummaryResponse {
        requireNonNull(receivedFrom);
        requireNonNull(receivedTo);
        statusCounts = List.copyOf(requireNonNull(statusCounts));
    }

    public static EmailOperationsStatusSummaryResponse from(InboundEmailStatusSummary summary) {
        requireNonNull(summary);

        return new EmailOperationsStatusSummaryResponse(
                summary.receivedFrom(),
                summary.receivedTo(),
                summary.totalCount(),
                summary.statusCounts().stream()
                        .map(InboundEmailStatusCountResponse::from)
                        .toList()
        );
    }

    public record InboundEmailStatusCountResponse(
            String status,
            long count
    ) {

        public static InboundEmailStatusCountResponse from(InboundEmailStatusCount statusCount) {
            requireNonNull(statusCount);

            return new InboundEmailStatusCountResponse(
                    statusCount.status().name(),
                    statusCount.count()
            );
        }
    }
}
