package com.sungho.letterpick.newsletter.application.provided;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

public record EmailOperationsQueueStatus(
        Instant checkedAt,
        QueueStatusCheckStatus status,
        MainQueueSnapshot mainQueue,
        DeadLetterQueueSnapshot deadLetterQueue,
        String failureReason
) {

    public EmailOperationsQueueStatus {
        requireNonNull(checkedAt);
        requireNonNull(status);
        if (status == QueueStatusCheckStatus.AVAILABLE) {
            requireNonNull(mainQueue);
            requireNonNull(deadLetterQueue);
            if (failureReason != null) {
                throw new IllegalArgumentException("failureReason must be null when queue status is available");
            }
        }
        if (status == QueueStatusCheckStatus.UNAVAILABLE) {
            requireNonNull(failureReason, "failureReason must not be null when queue status is unavailable");
            if (mainQueue != null || deadLetterQueue != null) {
                throw new IllegalArgumentException("queue snapshots must be null when queue status is unavailable");
            }
        }
    }

    public static EmailOperationsQueueStatus available(
            Instant checkedAt,
            MainQueueSnapshot mainQueue,
            DeadLetterQueueSnapshot deadLetterQueue
    ) {
        return new EmailOperationsQueueStatus(
                checkedAt,
                QueueStatusCheckStatus.AVAILABLE,
                mainQueue,
                deadLetterQueue,
                null
        );
    }

    public static EmailOperationsQueueStatus unavailable(Instant checkedAt, String failureReason) {
        return new EmailOperationsQueueStatus(
                checkedAt,
                QueueStatusCheckStatus.UNAVAILABLE,
                null,
                null,
                failureReason
        );
    }

    public enum QueueStatusCheckStatus {
        AVAILABLE,
        UNAVAILABLE
    }

    public record MainQueueSnapshot(
            long availableMessageCount,
            long inFlightMessageCount,
            long delayedMessageCount
    ) {

        public MainQueueSnapshot {
            requireNonNegative(availableMessageCount, "availableMessageCount");
            requireNonNegative(inFlightMessageCount, "inFlightMessageCount");
            requireNonNegative(delayedMessageCount, "delayedMessageCount");
        }
    }

    public record DeadLetterQueueSnapshot(
            long availableMessageCount
    ) {

        public DeadLetterQueueSnapshot {
            requireNonNegative(availableMessageCount, "availableMessageCount");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
