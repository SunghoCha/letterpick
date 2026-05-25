package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record EmailOperationsInboundEmailsResponse(
        List<InboundEmailItemResponse> items,
        PageResponse page
) {

    public EmailOperationsInboundEmailsResponse {
        items = List.copyOf(requireNonNull(items));
        requireNonNull(page);
    }

    public static EmailOperationsInboundEmailsResponse from(Slice<InboundEmailAdminItem> items) {
        requireNonNull(items);

        return new EmailOperationsInboundEmailsResponse(
                items.getContent().stream()
                        .map(InboundEmailItemResponse::from)
                        .toList(),
                PageResponse.from(items)
        );
    }

    public record InboundEmailItemResponse(
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

        public static InboundEmailItemResponse from(InboundEmailAdminItem item) {
            requireNonNull(item);

            return new InboundEmailItemResponse(
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
