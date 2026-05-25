package com.sungho.letterpick.newsletter.application.required;

import static java.util.Objects.requireNonNull;

public record QueueStatusSnapshot(
        MainQueueSnapshot mainQueue,
        DeadLetterQueueSnapshot deadLetterQueue
) {

    public QueueStatusSnapshot {
        requireNonNull(mainQueue);
        requireNonNull(deadLetterQueue);
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
