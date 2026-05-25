package com.sungho.letterpick.newsletter.application.provided;

import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record InboundEmailActionRequiredItem(
        Long inboundEmailId,
        Instant receivedAt,
        InboundEmailStatus status,
        String senderEmail,
        String recipientAddress,
        String subject,
        Long memberId,
        Long newsletterId,
        String messageKey,
        String rawReference
) {

    public InboundEmailActionRequiredItem {
        requireNonNull(inboundEmailId);
        requireNonNull(receivedAt);
        requireNonNull(status);
        requireNonNull(senderEmail);
        requireNonNull(recipientAddress);
        requireNonNull(subject);
        requireNonNull(messageKey);
        requireNonNull(rawReference);
    }
}
