package com.sungho.letterpick.trending.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import com.sungho.letterpick.trending.inbox.InboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultTrendingMessageProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private static final String QUEUE_NAME = "letterpick-test-trending-lifecycle-events";

    @Mock
    private InboxEventStore inboxEventStore;

    @Mock
    private TrendingEventProcessingService eventProcessingService;

    private DefaultTrendingMessageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DefaultTrendingMessageProcessor(
                OBJECT_MAPPER,
                inboxEventStore,
                eventProcessingService
        );
    }

    @Test
    @DisplayName("RECEIVED 메시지는 inbox에 기록한 뒤 event processing service로 전달한다")
    void process_received_message() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-1");
        given(inboxEventStore.receive(any(), eq(QUEUE_NAME), anyString()))
                .willReturn(InboxEventStatus.RECEIVED);

        // when
        processor.process(message, QUEUE_NAME);

        // then
        ArgumentCaptor<EventEnvelope<JsonNode>> envelopeCaptor = envelopeCaptor();
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(inboxEventStore).receive(envelopeCaptor.capture(), eq(QUEUE_NAME), payloadCaptor.capture());

        EventEnvelope<JsonNode> envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventId()).isEqualTo("event-1");
        assertThat(envelope.eventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value());
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.source()).isEqualTo("letterpick");
        assertThat(envelope.traceId()).isEqualTo("trace-1");

        JsonNode storedPayload = OBJECT_MAPPER.readTree(payloadCaptor.getValue());
        assertThat(storedPayload.path("issueId").asLong()).isEqualTo(1L);
        assertThat(storedPayload.path("newsletterId").asLong()).isEqualTo(2L);
        assertThat(storedPayload.path("category").asText()).isEqualTo("TECH");

        verify(eventProcessingService).process(envelope);
    }

    @Test
    @DisplayName("이미 PROCESSED인 메시지는 event processing service로 전달하지 않는다")
    void skip_already_processed_message() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-2");
        given(inboxEventStore.receive(any(), eq(QUEUE_NAME), anyString()))
                .willReturn(InboxEventStatus.PROCESSED);

        // when
        processor.process(message, QUEUE_NAME);

        // then
        verify(eventProcessingService, never()).process(any());
    }

    @Test
    @DisplayName("event processing 실패 시 inbox를 FAILED로 기록하고 예외를 유지한다")
    void mark_failed_and_rethrow_when_event_processing_fails() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-3");
        RuntimeException failure = new IllegalStateException("handler failed");
        given(inboxEventStore.receive(any(), eq(QUEUE_NAME), anyString()))
                .willReturn(InboxEventStatus.RECEIVED);
        willThrow(failure).given(eventProcessingService).process(any());

        // when & then
        assertThatThrownBy(() -> processor.process(message, QUEUE_NAME))
                .isSameAs(failure);
        verify(inboxEventStore).markFailed("event-3", failure);
    }

    @Test
    @DisplayName("envelope 역직렬화 실패 시 inbox에 기록하지 않는다")
    void reject_invalid_message_body() {
        // when & then
        assertThatThrownBy(() -> processor.process("{", QUEUE_NAME))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize trending event envelope");
        verifyNoInteractions(inboxEventStore, eventProcessingService);
    }

    private String publicIssueAvailableMessage(String eventId) throws Exception {
        EventEnvelope<PublicIssueAvailablePayload> envelope = new EventEnvelope<>(
                eventId,
                TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                new PublicIssueAvailablePayload(
                        1L,
                        2L,
                        "TECH",
                        Instant.parse("2050-06-05T00:59:00Z")
                )
        );
        return OBJECT_MAPPER.writeValueAsString(envelope);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<EventEnvelope<JsonNode>> envelopeCaptor() {
        return ArgumentCaptor.forClass(EventEnvelope.class);
    }
}
