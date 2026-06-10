package com.sungho.letterpick.event.trending;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sungho.letterpick.event.EventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PublicIssueRemovedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED 이벤트 계약을 직렬화하고 역직렬화한다")
    void serializesPublicIssueRemovedEventContract() throws Exception {
        PublicIssueRemovedPayload payload = new PublicIssueRemovedPayload(1L);
        EventEnvelope<PublicIssueRemovedPayload> event = new EventEnvelope<>(
                "event-1",
                TrendingEventType.PUBLIC_ISSUE_REMOVED.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );

        String json = objectMapper.writeValueAsString(event);

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("eventId").asText()).isEqualTo("event-1");
        assertThat(root.path("eventType").asText()).isEqualTo("PUBLIC_ISSUE_REMOVED");
        assertThat(root.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(root.path("source").asText()).isEqualTo("letterpick");
        assertThat(root.path("occurredAt").asText()).isEqualTo("2050-06-05T01:00:00Z");
        assertThat(root.path("traceId").asText()).isEqualTo("trace-1");
        assertThat(root.path("payload").path("issueId").asLong()).isEqualTo(1L);

        EventEnvelope<PublicIssueRemovedPayload> restored = objectMapper.readValue(
                json,
                new TypeReference<>() {
                }
        );
        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED payload는 issueId를 필수로 요구한다")
    void rejectsNullIssueId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PublicIssueRemovedPayload(null))
                .withMessage("issueId must not be null");
    }
}
