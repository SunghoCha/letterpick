package com.sungho.letterpick.common.outbox;

import java.time.Instant;
import java.util.Objects;

public record OutboxMessageRecordRequest(
        String eventId,
        OutboxMessageType type,
        String aggregateId,
        Object payload,
        Instant occurredAt
) {

    public OutboxMessageRecordRequest {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
