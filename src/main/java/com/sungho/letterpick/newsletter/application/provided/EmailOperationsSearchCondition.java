package com.sungho.letterpick.newsletter.application.provided;

import java.time.Instant;

public record EmailOperationsSearchCondition(
        Instant receivedFrom,
        Instant receivedTo
) {

    public EmailOperationsSearchCondition {
        if ((receivedFrom == null) != (receivedTo == null)) {
            throw new IllegalArgumentException("receivedFrom and receivedTo must be provided together");
        }
        if (receivedFrom != null && !receivedFrom.isBefore(receivedTo)) {
            throw new IllegalArgumentException("receivedFrom must be before receivedTo");
        }
    }

    public static EmailOperationsSearchCondition empty() {
        return new EmailOperationsSearchCondition(null, null);
    }

    public static EmailOperationsSearchCondition receivedAtRange(Instant receivedFrom, Instant receivedTo) {
        return new EmailOperationsSearchCondition(receivedFrom, receivedTo);
    }

    public boolean hasReceivedRange() {
        return receivedFrom != null;
    }
}
