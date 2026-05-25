package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailActionRequiredItem;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record EmailOperationsActionRequiredResponse(
        List<ActionRequiredItemResponse> items,
        PageResponse page
) {

    public EmailOperationsActionRequiredResponse {
        items = List.copyOf(requireNonNull(items));
        requireNonNull(page);
    }

    public static EmailOperationsActionRequiredResponse from(Slice<InboundEmailActionRequiredItem> items) {
        requireNonNull(items);

        return new EmailOperationsActionRequiredResponse(
                items.getContent().stream()
                        .map(ActionRequiredItemResponse::from)
                        .toList(),
                PageResponse.from(items)
        );
    }

    public record ActionRequiredItemResponse(
            Long inboundEmailId,
            Instant receivedAt,
            String status,
            String senderEmail,
            String recipientAddress,
            String subject,
            Long memberId,
            Long newsletterId,
            String messageKey,
            String rawReference
    ) {

        public static ActionRequiredItemResponse from(InboundEmailActionRequiredItem item) {
            requireNonNull(item);

            return new ActionRequiredItemResponse(
                    item.inboundEmailId(),
                    item.receivedAt(),
                    item.status().name(),
                    item.senderEmail(),
                    item.recipientAddress(),
                    item.subject(),
                    item.memberId(),
                    item.newsletterId(),
                    item.messageKey(),
                    item.rawReference()
            );
        }
    }
}
