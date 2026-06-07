package com.sungho.letterpick.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxMessageRelayTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-06T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private OutboxMessagePublisher outboxMessagePublisher;

    private DefaultOutboxMessageRelay relay;

    @BeforeEach
    void setUp() {
        relay = new DefaultOutboxMessageRelay(
                outboxMessageRepository,
                outboxMessagePublisher,
                CLOCK
        );
    }

    @Test
    @DisplayName("eventId로 찾은 outbox 메시지를 발행하고 성공하면 삭제한다")
    void publishByEventIdDeletesMessageWhenPublishSucceeds() {
        OutboxMessage message = outboxMessage("event-1");
        given(outboxMessageRepository.findByEventId("event-1"))
                .willReturn(Optional.of(message));

        relay.publishByEventId("event-1");

        verify(outboxMessagePublisher).publish(message);
        verify(outboxMessageRepository).delete(message);
    }

    @Test
    @DisplayName("eventId로 찾은 outbox 메시지 발행에 실패하면 재시도 정보를 기록한다")
    void publishByEventIdMarksMessageFailedWhenPublishFails() {
        OutboxMessage message = outboxMessage("event-1");
        given(outboxMessageRepository.findByEventId("event-1"))
                .willReturn(Optional.of(message));
        willThrow(new RuntimeException("SQS unavailable"))
                .given(outboxMessagePublisher).publish(message);

        relay.publishByEventId("event-1");

        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.FAILED);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getLastError()).contains("RuntimeException", "SQS unavailable");
        assertThat(message.getNextAttemptAt()).isEqualTo(CLOCK.instant().plus(Duration.ofMinutes(1)));
        assertThat(message.getUpdatedAt()).isEqualTo(CLOCK.instant());
        verify(outboxMessageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("재발행 대상 outbox 메시지를 제한 개수만큼 조회해 발행한다")
    void publishDueMessagesPublishesDueMessages() {
        OutboxMessage first = outboxMessage("event-1");
        OutboxMessage second = outboxMessage("event-2");
        given(outboxMessageRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(List.of(OutboxMessageStatus.PENDING, OutboxMessageStatus.FAILED)),
                eq(CLOCK.instant()),
                any(Pageable.class)
        )).willReturn(List.of(first, second));

        int published = relay.publishDueMessages(100);

        assertThat(published).isEqualTo(2);
        verify(outboxMessagePublisher).publish(first);
        verify(outboxMessagePublisher).publish(second);
        verify(outboxMessageRepository).delete(first);
        verify(outboxMessageRepository).delete(second);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(outboxMessageRepository).findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(List.of(OutboxMessageStatus.PENDING, OutboxMessageStatus.FAILED)),
                eq(CLOCK.instant()),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("재발행 제한 개수는 양수여야 한다")
    void publishDueMessagesRequiresPositiveLimit() {
        assertThatThrownBy(() -> relay.publishDueMessages(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be positive");
    }

    private OutboxMessage outboxMessage(String eventId) {
        return OutboxMessage.create(
                eventId,
                "letterpick-test-trending-lifecycle-events",
                "PUBLIC_ISSUE_AVAILABLE",
                1,
                "letterpick",
                "NEWSLETTER_ISSUE",
                "10",
                "{\"issueId\":10}",
                Instant.parse("2050-06-06T00:00:00Z"),
                "trace-1",
                CLOCK.instant()
        );
    }
}
