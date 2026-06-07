package com.sungho.letterpick.trending.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inbox_event_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_inbox_event_status_received", columnList = "status, received_at"),
                @Index(name = "idx_inbox_event_type_occurred", columnList = "event_type, occurred_at")
        })
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "queue_name", nullable = false, length = 100)
    private String queueName;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private InboxEventStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private InboxEvent(String eventId, String eventType, int schemaVersion, String source,
                       Instant occurredAt, String traceId, String queueName, String payload, Instant now) {
        this.eventId = requireNonNull(eventId);
        this.eventType = requireNonNull(eventType);
        this.schemaVersion = schemaVersion;
        this.source = requireNonNull(source);
        this.occurredAt = requireNonNull(occurredAt);
        this.traceId = traceId;
        this.queueName = requireNonNull(queueName);
        this.payload = requireNonNull(payload);
        this.status = InboxEventStatus.RECEIVED;
        this.receivedAt = requireNonNull(now);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static InboxEvent receive(String eventId, String eventType, int schemaVersion, String source,
                                     Instant occurredAt, String traceId, String queueName, String payload,
                                     Instant now) {
        return new InboxEvent(eventId, eventType, schemaVersion, source,
                occurredAt, traceId, queueName, payload, now);
    }

    public void markProcessed(Instant now) {
        this.status = InboxEventStatus.PROCESSED;
        this.processedAt = requireNonNull(now);
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailed(String lastError, Instant now) {
        this.status = InboxEventStatus.FAILED;
        this.lastError = requireNonNull(lastError);
        this.updatedAt = requireNonNull(now);
    }
}
