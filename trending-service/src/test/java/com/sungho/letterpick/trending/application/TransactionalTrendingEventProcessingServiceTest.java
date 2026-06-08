package com.sungho.letterpick.trending.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.trending.inbox.InboxEvent;
import com.sungho.letterpick.trending.inbox.InboxEventRepository;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionalTrendingEventProcessingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-08T01:00:00Z"), ZoneOffset.UTC);
    private static final String EVENT_TYPE = "TEST_EVENT";

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Mock
    private TrendingEventHandler handler;

    private TransactionalTrendingEventProcessingService service;

    @BeforeEach
    void setUp() {
        given(handler.eventType()).willReturn(EVENT_TYPE);
        service = new TransactionalTrendingEventProcessingService(
                inboxEventRepository,
                List.of(handler),
                CLOCK
        );
    }

    @Test
    @DisplayName("RECEIVED inbox 이벤트는 handler 실행 후 PROCESSED로 기록한다")
    void process_received_inbox_event() {
        // given
        EventEnvelope<JsonNode> envelope = envelope("event-1");
        InboxEvent inboxEvent = inboxEvent("event-1");
        given(inboxEventRepository.findByEventIdForUpdate("event-1"))
                .willReturn(Optional.of(inboxEvent));

        // when
        service.process(envelope);

        // then
        verify(handler).handle(envelope);
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEvent.getProcessedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("이미 PROCESSED인 inbox 이벤트는 handler를 다시 실행하지 않는다")
    void skip_already_processed_inbox_event() {
        // given
        EventEnvelope<JsonNode> envelope = envelope("event-2");
        InboxEvent inboxEvent = inboxEvent("event-2");
        inboxEvent.markProcessed(CLOCK.instant());
        given(inboxEventRepository.findByEventIdForUpdate("event-2"))
                .willReturn(Optional.of(inboxEvent));

        // when
        service.process(envelope);

        // then
        verify(handler, never()).handle(any());
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEvent.getProcessedAt()).isEqualTo(CLOCK.instant());
    }

    private EventEnvelope<JsonNode> envelope(String eventId) {
        return new EventEnvelope<>(
                eventId,
                EVENT_TYPE,
                1,
                "letterpick",
                Instant.parse("2050-06-08T00:59:00Z"),
                "trace-1",
                JsonNodeFactory.instance.objectNode()
        );
    }

    private InboxEvent inboxEvent(String eventId) {
        return InboxEvent.receive(
                eventId,
                EVENT_TYPE,
                1,
                "letterpick",
                Instant.parse("2050-06-08T00:59:00Z"),
                "trace-1",
                "letterpick-test-trending-lifecycle-events",
                "{}",
                CLOCK.instant()
        );
    }
}
