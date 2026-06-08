package com.sungho.letterpick.trending.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import com.sungho.letterpick.trending.inbox.InboxEventStore;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.util.Objects.requireNonNull;

@Service
public class DefaultTrendingMessageProcessor implements TrendingMessageProcessor {

    private static final TypeReference<EventEnvelope<JsonNode>> ENVELOPE_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final InboxEventStore inboxEventStore;
    private final TrendingEventProcessingService eventProcessingService;

    public DefaultTrendingMessageProcessor(ObjectMapper objectMapper,
                                           InboxEventStore inboxEventStore,
                                           TrendingEventProcessingService eventProcessingService) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
        this.inboxEventStore = requireNonNull(inboxEventStore, "inboxEventStore must not be null");
        this.eventProcessingService = requireNonNull(eventProcessingService, "eventProcessingService must not be null");
    }

    @Override
    public void process(String messageBody, String queueName) {
        EventEnvelope<JsonNode> envelope = readEnvelope(messageBody);
        String payload = writePayload(envelope.payload());

        InboxEventStatus status = inboxEventStore.receive(envelope, queueName, payload);
        if (status == InboxEventStatus.PROCESSED) {
            return;
        }

        try {
            eventProcessingService.process(envelope);
        } catch (RuntimeException e) {
            inboxEventStore.markFailed(envelope.eventId(), e);
            throw e;
        }
    }

    private EventEnvelope<JsonNode> readEnvelope(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, ENVELOPE_TYPE);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to deserialize trending event envelope", e);
        }
    }

    private String writePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to serialize trending event payload", e);
        }
    }
}
