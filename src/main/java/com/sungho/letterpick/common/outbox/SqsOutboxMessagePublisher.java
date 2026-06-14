package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.event.EventEnvelope;
import io.awspring.cloud.sqs.operations.SqsOperations;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class SqsOutboxMessagePublisher implements OutboxMessagePublisher {

    private final SqsOperations sqsOperations;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OutboxMessage message) {
        sqsOperations.send(options -> options
                .queue(message.getDestination())
                .payload(messageBody(message))
        );
    }

    private String messageBody(OutboxMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                    message.getEventId(),
                    message.getEventType(),
                    message.getSchemaVersion(),
                    message.getSource(),
                    message.getOccurredAt(),
                    message.getTraceId(),
                    payload
            );
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new OutboxMessagePublishException(e);
        }
    }
}
