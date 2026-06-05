package com.sungho.letterpick.event;

import java.time.Instant;
import java.util.Objects;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        int schemaVersion,
        String source,
        Instant occurredAt,
        String traceId,
        T payload
) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
    }
}
