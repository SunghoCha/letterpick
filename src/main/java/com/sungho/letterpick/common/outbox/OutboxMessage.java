package com.sungho.letterpick.common.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_outbox_event_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_outbox_event_publish", columnList = "status, next_attempt_at, created_at")
        })
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxMessageStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private OutboxMessage(String eventId, String destination, String eventType, int schemaVersion,
                          String source, String aggregateType, String aggregateId, String payload,
                          Instant occurredAt, String traceId, Instant nextAttemptAt, Instant createdAt) {
        this.eventId = requireNonNull(eventId);
        this.destination = requireNonNull(destination);
        this.eventType = requireNonNull(eventType);
        this.schemaVersion = schemaVersion;
        this.source = requireNonNull(source);
        this.aggregateType = requireNonNull(aggregateType);
        this.aggregateId = requireNonNull(aggregateId);
        this.payload = requireNonNull(payload);
        this.occurredAt = requireNonNull(occurredAt);
        this.traceId = traceId;
        this.status = OutboxMessageStatus.PENDING;
        this.retryCount = 0;
        this.nextAttemptAt = requireNonNull(nextAttemptAt);
        this.createdAt = requireNonNull(createdAt);
        this.updatedAt = createdAt;
    }

    public static OutboxMessage create(String eventId, String destination, String eventType, int schemaVersion,
                                       String source, String aggregateType, String aggregateId, String payload,
                                       Instant occurredAt, String traceId, Instant now) {
        return create(eventId, destination, eventType, schemaVersion, source, aggregateType,
                aggregateId, payload, occurredAt, traceId, now, now);
    }

    public static OutboxMessage create(String eventId, String destination, String eventType, int schemaVersion,
                                       String source, String aggregateType, String aggregateId, String payload,
                                       Instant occurredAt, String traceId, Instant nextAttemptAt, Instant now) {
        return new OutboxMessage(eventId, destination, eventType, schemaVersion, source, aggregateType,
                aggregateId, payload, occurredAt, traceId, nextAttemptAt, now);
    }

    public void markFailed(String lastError, Instant nextAttemptAt, Instant now) {
        this.status = OutboxMessageStatus.FAILED;
        this.retryCount++;
        this.lastError = lastError;
        this.nextAttemptAt = requireNonNull(nextAttemptAt);
        this.updatedAt = requireNonNull(now);
    }
}
