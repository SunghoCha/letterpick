package com.sungho.letterpick.trending.score.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
public class TrendingScoreMessageProcessor {

    private static final TypeReference<EventEnvelope<JsonNode>> ENVELOPE_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Map<String, TrendingScoreEventHandler> handlers;

    public TrendingScoreMessageProcessor(ObjectMapper objectMapper,
                                         List<TrendingScoreEventHandler> handlers) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
        this.handlers = requireNonNull(handlers, "handlers must not be null").stream()
                .collect(Collectors.toUnmodifiableMap(
                        TrendingScoreEventHandler::eventType,
                        Function.identity()
                ));
    }

    public void process(String messageBody) {
        EventEnvelope<JsonNode> envelope = readEnvelope(messageBody);
        TrendingScoreEventHandler handler = handlers.get(envelope.eventType());
        if (handler == null) {
            throw new TrendingMessageProcessingException("unsupported trending score event type: "
                    + envelope.eventType());
        }

        handler.handle(envelope);
    }

    private EventEnvelope<JsonNode> readEnvelope(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, ENVELOPE_TYPE);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to deserialize trending score event envelope", e);
        }
    }
}
