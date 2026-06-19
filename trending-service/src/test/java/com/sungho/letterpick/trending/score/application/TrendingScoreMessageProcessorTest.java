package com.sungho.letterpick.trending.score.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendingScoreMessageProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();

    @Mock
    private TrendingScoreEventHandler handler;

    private TrendingScoreMessageProcessor processor;

    @BeforeEach
    void setUp() {
        when(handler.eventType()).thenReturn(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());
        processor = new TrendingScoreMessageProcessor(OBJECT_MAPPER, List.of(handler));
    }

    @Test
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED 메시지를 조회수 handler로 전달한다")
    void process_issue_view_count_updated_message() throws Exception {
        // given
        String message = message(
                "event-score-1",
                TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
                new IssueViewCountUpdatedPayload(10L, 150L)
        );

        // when
        processor.process(message);

        // then
        ArgumentCaptor<EventEnvelope<JsonNode>> envelopeCaptor = envelopeCaptor();
        verify(handler).handle(envelopeCaptor.capture());

        EventEnvelope<JsonNode> envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventId()).isEqualTo("event-score-1");
        assertThat(envelope.eventType()).isEqualTo(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());
        assertThat(envelope.payload().path("issueId").asLong()).isEqualTo(10L);
        assertThat(envelope.payload().path("viewCount").asLong()).isEqualTo(150L);
    }

    @Test
    @DisplayName("score queue에서 지원하지 않는 eventType은 거부한다")
    void reject_unsupported_event_type() throws Exception {
        // given
        String message = message(
                "event-lifecycle-1",
                TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
                new PublicIssueAvailablePayload(
                        10L,
                        20L,
                        "TECH",
                        Instant.parse("2050-06-05T00:59:00Z")
                )
        );

        // when & then
        assertThatThrownBy(() -> processor.process(message))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("unsupported trending score event type");
        verify(handler, never()).handle(any());
    }

    @Test
    @DisplayName("envelope 역직렬화에 실패하면 handler를 호출하지 않는다")
    void reject_invalid_message_body() {
        // when & then
        assertThatThrownBy(() -> processor.process("{"))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize trending score event envelope");
        verify(handler, never()).handle(any());
    }

    @Test
    @DisplayName("같은 eventType의 score handler가 중복 등록되면 생성에 실패한다")
    void reject_duplicate_score_event_handler() {
        // given
        TrendingScoreEventHandler duplicateHandler = mock(TrendingScoreEventHandler.class);
        when(duplicateHandler.eventType()).thenReturn(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());

        // when & then
        assertThatThrownBy(() -> new TrendingScoreMessageProcessor(OBJECT_MAPPER, List.of(handler, duplicateHandler)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate trending score event handler")
                .hasMessageContaining(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());
    }

    private String message(String eventId, String eventType, Object payload) throws Exception {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );
        return OBJECT_MAPPER.writeValueAsString(envelope);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<EventEnvelope<JsonNode>> envelopeCaptor() {
        return ArgumentCaptor.forClass(EventEnvelope.class);
    }
}
