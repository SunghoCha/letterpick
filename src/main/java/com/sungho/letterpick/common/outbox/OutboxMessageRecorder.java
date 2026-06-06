package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.common.logging.MdcInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxMessageRecorder {

    private static final String SOURCE = "letterpick";
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMinutes(1);

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxMessageRecorder(OutboxMessageRepository outboxMessageRepository,
                                 ObjectMapper objectMapper,
                                 Clock clock) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OutboxMessage record(OutboxMessageRecordRequest request) {
        String payload = serializePayload(request.payload());
        Instant now = clock.instant();
        Instant nextAttemptAt = now.plus(INITIAL_RETRY_DELAY);
        OutboxMessageType type = request.type();

        OutboxMessage message = OutboxMessage.create(
                request.eventId(),
                type.destination(),
                type.eventType(),
                type.schemaVersion(),
                SOURCE,
                type.aggregateType(),
                request.aggregateId(),
                payload,
                request.occurredAt(),
                resolveTraceId(),
                nextAttemptAt,
                now
        );

        return outboxMessageRepository.save(message);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new OutboxPayloadSerializationException(e);
        }
    }

    private String resolveTraceId() {
        String requestId = MDC.get(MdcInterceptor.REQUEST_ID);
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
