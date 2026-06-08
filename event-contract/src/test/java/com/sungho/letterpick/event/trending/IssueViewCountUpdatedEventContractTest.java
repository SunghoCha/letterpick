package com.sungho.letterpick.event.trending;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sungho.letterpick.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueViewCountUpdatedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesIssueViewCountUpdatedEventContract() throws Exception {
        IssueViewCountUpdatedPayload payload = new IssueViewCountUpdatedPayload(
                1L,
                150L
        );
        EventEnvelope<IssueViewCountUpdatedPayload> event = new EventEnvelope<>(
                "event-1",
                TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );

        String json = objectMapper.writeValueAsString(event);

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("eventId").asText()).isEqualTo("event-1");
        assertThat(root.path("eventType").asText()).isEqualTo("ISSUE_VIEW_COUNT_UPDATED");
        assertThat(root.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(root.path("source").asText()).isEqualTo("letterpick");
        assertThat(root.path("occurredAt").asText()).isEqualTo("2050-06-05T01:00:00Z");
        assertThat(root.path("traceId").asText()).isEqualTo("trace-1");
        assertThat(root.path("payload").path("issueId").asLong()).isEqualTo(1L);
        assertThat(root.path("payload").path("viewCount").asLong()).isEqualTo(150L);

        EventEnvelope<IssueViewCountUpdatedPayload> restored = objectMapper.readValue(
                json,
                new TypeReference<>() {
                }
        );
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void rejectsNegativeViewCount() {
        assertThatThrownBy(() -> new IssueViewCountUpdatedPayload(1L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("viewCount must not be negative");
    }
}
