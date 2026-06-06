package com.sungho.letterpick.event.trending;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sungho.letterpick.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicIssueAvailableEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesPublicIssueAvailableEventContract() throws Exception {
        PublicIssueAvailablePayload payload = new PublicIssueAvailablePayload(
                1L,
                2L,
                "TECH",
                Instant.parse("2050-06-05T00:59:00Z")
        );
        EventEnvelope<PublicIssueAvailablePayload> event = new EventEnvelope<>(
                "event-1",
                TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );

        String json = objectMapper.writeValueAsString(event);

        JsonNode root = objectMapper.readTree(json);
        assertEquals("event-1", root.path("eventId").asText());
        assertEquals("PUBLIC_ISSUE_AVAILABLE", root.path("eventType").asText());
        assertEquals(1, root.path("schemaVersion").asInt());
        assertEquals("letterpick", root.path("source").asText());
        assertEquals("2050-06-05T01:00:00Z", root.path("occurredAt").asText());
        assertEquals("trace-1", root.path("traceId").asText());
        assertEquals(1L, root.path("payload").path("issueId").asLong());
        assertEquals(2L, root.path("payload").path("newsletterId").asLong());
        assertEquals("TECH", root.path("payload").path("category").asText());
        assertEquals("2050-06-05T00:59:00Z", root.path("payload").path("publicFeedCollectedAt").asText());

        EventEnvelope<PublicIssueAvailablePayload> restored = objectMapper.readValue(
                json,
                new TypeReference<>() {
                }
        );
        assertEquals(event, restored);
    }
}
