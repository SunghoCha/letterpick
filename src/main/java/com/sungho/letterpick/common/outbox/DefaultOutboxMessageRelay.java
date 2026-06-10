package com.sungho.letterpick.common.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnBean(OutboxMessagePublisher.class)
@RequiredArgsConstructor
public class DefaultOutboxMessageRelay implements OutboxMessageRelay {

    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final List<OutboxMessageStatus> PUBLISHABLE_STATUSES = List.of(
            OutboxMessageStatus.PENDING,
            OutboxMessageStatus.FAILED
    );

    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void publishByEventId(String eventId) {
        outboxMessageRepository.findByEventId(eventId)
                .ifPresent(this::publish);
    }

    @Override
    @Transactional
    public int publishDueMessages(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }

        List<OutboxMessage> messages = outboxMessageRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        PUBLISHABLE_STATUSES,
                        clock.instant(),
                        PageRequest.of(0, limit)
                );

        messages.forEach(this::publish);
        return messages.size();
    }

    private void publish(OutboxMessage message) {
        try {
            outboxMessagePublisher.publish(message);
        } catch (Exception e) {
            Instant now = clock.instant();
            message.markFailed(errorMessage(e), now.plus(RETRY_DELAY), now);
            return;
        }

        outboxMessageRepository.delete(message);
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getName();
        }
        return e.getClass().getName() + ": " + message;
    }
}
