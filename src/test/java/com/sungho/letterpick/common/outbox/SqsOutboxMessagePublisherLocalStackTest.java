package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.LetterPickAwsTestConfiguration;
import com.sungho.letterpick.LetterPickTestConfiguration;
import io.awspring.cloud.sqs.operations.SqsOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import({LetterPickTestConfiguration.class, LetterPickAwsTestConfiguration.class})
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=true",
        "letterpick.outbox.publish.enabled=true",
        "letterpick.outbox.retry.enabled=false",
        "letterpick.mail.sqs-listener.enabled=false"
})
@ActiveProfiles("test")
class SqsOutboxMessagePublisherLocalStackTest {

    private static final String TRENDING_LIFECYCLE_QUEUE_NAME = "letterpick-test-trending-lifecycle-events";

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private SqsOperations sqsOperations;

    @Autowired
    private OutboxMessagePublisher outboxMessagePublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                        .queueName(TRENDING_LIFECYCLE_QUEUE_NAME)
                        .build())
                .join();
    }

    @Test
    @DisplayName("outbox 메시지를 SQS에 이벤트 envelope 형태로 발행한다")
    void publish_sends_event_envelope_message_to_sqs() throws JacksonException {
        Instant occurredAt = Instant.parse("2026-06-06T00:00:00Z");
        OutboxMessage message = OutboxMessage.create(
                "event-1",
                TRENDING_LIFECYCLE_QUEUE_NAME,
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.eventType(),
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.schemaVersion(),
                "letterpick",
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.aggregateType(),
                "100",
                """
                        {
                          "issueId": 100,
                          "newsletterId": 200,
                          "category": "TECH",
                          "publicFeedCollectedAt": "2026-06-06T00:00:00Z"
                        }
                        """,
                occurredAt,
                "trace-1",
                occurredAt
        );

        outboxMessagePublisher.publish(message);

        JsonNode body = objectMapper.readTree(receiveMessageBody());
        assertThat(body.path("eventId").asText()).isEqualTo("event-1");
        assertThat(body.path("eventType").asText()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.eventType());
        assertThat(body.path("schemaVersion").asText()).isEqualTo("1");
        assertThat(body.path("source").asText()).isEqualTo("letterpick");
        assertThat(body.path("occurredAt").asText()).isEqualTo("2026-06-06T00:00:00Z");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-1");
        assertThat(body.path("payload").path("issueId").asText()).isEqualTo("100");
        assertThat(body.path("payload").path("newsletterId").asText()).isEqualTo("200");
        assertThat(body.path("payload").path("category").asText()).isEqualTo("TECH");
        assertThat(body.path("payload").path("publicFeedCollectedAt").asText()).isEqualTo("2026-06-06T00:00:00Z");
    }

    private String receiveMessageBody() {
        return sqsOperations.receive(options -> options
                        .queue(TRENDING_LIFECYCLE_QUEUE_NAME)
                        .pollTimeout(Duration.ofSeconds(1)), String.class)
                .orElseThrow(() -> new AssertionError("SQS 메시지를 수신하지 못했습니다."))
                .getPayload();
    }
}
