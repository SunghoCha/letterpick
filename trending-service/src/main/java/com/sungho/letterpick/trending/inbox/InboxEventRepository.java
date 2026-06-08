package com.sungho.letterpick.trending.inbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    Optional<InboxEvent> findByEventId(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from InboxEvent e where e.eventId = :eventId")
    Optional<InboxEvent> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Modifying
    @Query(value = """
            INSERT INTO inbox_event (
                event_id,
                event_type,
                schema_version,
                source,
                occurred_at,
                trace_id,
                queue_name,
                payload,
                status,
                received_at,
                created_at,
                updated_at
            ) VALUES (
                :eventId,
                :eventType,
                :schemaVersion,
                :source,
                :occurredAt,
                :traceId,
                :queueName,
                :payload,
                :status,
                :receivedAt,
                :createdAt,
                :updatedAt
            )
            ON DUPLICATE KEY UPDATE event_id = event_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("eventId") String eventId,
                        @Param("eventType") String eventType,
                        @Param("schemaVersion") int schemaVersion,
                        @Param("source") String source,
                        @Param("occurredAt") Instant occurredAt,
                        @Param("traceId") String traceId,
                        @Param("queueName") String queueName,
                        @Param("payload") String payload,
                        @Param("status") String status,
                        @Param("receivedAt") Instant receivedAt,
                        @Param("createdAt") Instant createdAt,
                        @Param("updatedAt") Instant updatedAt);
}
