package com.sungho.letterpick.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
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
                .ifPresentOrElse(
                        message -> {
                            if (publish(message)) {
                                log.info(
                                        "Outbox message published by eventId. eventId={}, eventType={}, aggregateId={}, destination={}",
                                        message.getEventId(),
                                        message.getEventType(),
                                        message.getAggregateId(),
                                        message.getDestination()
                                );
                            }
                        },
                        () -> log.debug("Outbox message not found for immediate publish. eventId={}", eventId)
                );
    }

    @Override
    @Transactional
    public int publishDueMessages(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }

        long startedAt = System.nanoTime();
        List<OutboxMessage> messages = outboxMessageRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        PUBLISHABLE_STATUSES,
                        clock.instant(),
                        PageRequest.of(0, limit)
                );

        if (messages.isEmpty()) {
            log.debug("Outbox relay batch skipped. requested={}, fetched=0", limit);
            return 0;
        }

        int publishedCount = 0;
        int failedCount = 0;
        for (OutboxMessage message : messages) {
            if (publish(message)) {
                publishedCount++;
            } else {
                failedCount++;
            }
        }

        log.info(
                "Outbox relay batch processed. requested={}, fetched={}, published={}, failed={}, durationMs={}",
                limit,
                messages.size(),
                publishedCount,
                failedCount,
                (System.nanoTime() - startedAt) / 1_000_000L
        );
        return messages.size();
    }

    private boolean publish(OutboxMessage message) {
        try {
            outboxMessagePublisher.publish(message);
        } catch (Exception e) {
            Instant now = clock.instant();
            message.markFailed(errorMessage(e), now.plus(RETRY_DELAY), now);
            log.warn(
                    "Outbox message publish failed. eventId={}, eventType={}, aggregateId={}, destination={}, retryCount={}, nextAttemptAt={}",
                    message.getEventId(),
                    message.getEventType(),
                    message.getAggregateId(),
                    message.getDestination(),
                    message.getRetryCount(),
                    message.getNextAttemptAt(),
                    e
            );
            return false;
        }

        outboxMessageRepository.delete(message);
        return true;
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getName();
        }
        return e.getClass().getName() + ": " + message;
    }
}
