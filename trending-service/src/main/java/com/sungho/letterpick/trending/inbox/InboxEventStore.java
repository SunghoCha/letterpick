package com.sungho.letterpick.trending.inbox;

import com.sungho.letterpick.event.EventEnvelope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

@Service
public class InboxEventStore {

    private final InboxEventRepository inboxEventRepository;
    private final Clock clock;

    public InboxEventStore(InboxEventRepository inboxEventRepository, Clock clock) {
        this.inboxEventRepository = requireNonNull(inboxEventRepository, "inboxEventRepository must not be null");
        this.clock = requireNonNull(clock, "clock must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InboxEventStatus receive(EventEnvelope<JsonNode> envelope, String queueName, String payload) {
        var now = clock.instant();
        inboxEventRepository.insertIfAbsent(
                envelope.eventId(),
                envelope.eventType(),
                envelope.schemaVersion(),
                envelope.source(),
                envelope.occurredAt(),
                envelope.traceId(),
                queueName,
                payload,
                InboxEventStatus.RECEIVED.name(),
                now,
                now,
                now
        );
        return inboxEventRepository.findByEventId(envelope.eventId())
                .orElseThrow(() -> new IllegalStateException("inbox event is not recorded: " + envelope.eventId()))
                .getStatus();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String eventId, RuntimeException failure) {
        inboxEventRepository.findByEventId(eventId)
                .ifPresent(inboxEvent -> inboxEvent.markFailed(formatFailure(failure), clock.instant()));
    }

    private String formatFailure(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return failure.getClass().getSimpleName() + ": " + message;
    }
}
