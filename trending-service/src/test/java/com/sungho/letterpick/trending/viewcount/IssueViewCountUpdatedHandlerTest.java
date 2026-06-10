package com.sungho.letterpick.trending.viewcount;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IssueViewCountUpdatedHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-10T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicIssueViewCountSnapshotRepository repository;

    private IssueViewCountUpdatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueViewCountUpdatedHandler(
                OBJECT_MAPPER,
                repository,
                CLOCK
        );
    }

    @Test
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED eventType을 처리한다")
    void supports_issue_view_count_updated_event_type() {
        assertThat(handler.eventType()).isEqualTo(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());
    }

    @Test
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED payload를 조회수 snapshot으로 저장한다")
    void upsert_view_count_snapshot() {
        // given
        Instant occurredAt = Instant.parse("2050-06-10T00:59:00Z");
        EventEnvelope<JsonNode> envelope = envelope(new IssueViewCountUpdatedPayload(1L, 150L), occurredAt);

        // when
        handler.handle(envelope);

        // then
        verify(repository).upsertSnapshot(
                1L,
                150L,
                occurredAt,
                CLOCK.instant()
        );
    }

    @Test
    @DisplayName("payload 필수 필드가 누락되면 조회수 snapshot을 저장하지 않는다")
    void reject_payload_with_missing_required_fields() {
        // given
        JsonNode invalidPayload = OBJECT_MAPPER.createObjectNode()
                .put("issueId", 1L);
        EventEnvelope<JsonNode> envelope = envelope(
                invalidPayload,
                Instant.parse("2050-06-10T00:59:00Z")
        );

        // when & then
        assertThatThrownBy(() -> handler.handle(envelope))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize ISSUE_VIEW_COUNT_UPDATED payload");
        verifyNoInteractions(repository);
    }

    private EventEnvelope<JsonNode> envelope(IssueViewCountUpdatedPayload payload, Instant occurredAt) {
        return envelope(OBJECT_MAPPER.valueToTree(payload), occurredAt);
    }

    private EventEnvelope<JsonNode> envelope(JsonNode payload, Instant occurredAt) {
        return new EventEnvelope<>(
                "event-1",
                TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
                1,
                "letterpick",
                occurredAt,
                "trace-1",
                payload
        );
    }
}
