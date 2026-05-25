package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import static java.util.Objects.requireNonNull;

import com.sungho.letterpick.newsletter.application.provided.EmailOperationsQueueStatus;
import java.time.Instant;

public record EmailOperationsQueueStatusResponse(
        Instant checkedAt,
        String status,
        MainQueueResponse mainQueue,
        DeadLetterQueueResponse deadLetterQueue,
        String failureReason
) {

    public EmailOperationsQueueStatusResponse {
        requireNonNull(checkedAt);
        requireNonNull(status);
    }

    public static EmailOperationsQueueStatusResponse from(EmailOperationsQueueStatus queueStatus) {
        requireNonNull(queueStatus);

        return new EmailOperationsQueueStatusResponse(
                queueStatus.checkedAt(),
                queueStatus.status().name(),
                MainQueueResponse.from(queueStatus.mainQueue()),
                DeadLetterQueueResponse.from(queueStatus.deadLetterQueue()),
                queueStatus.failureReason()
        );
    }

    public record MainQueueResponse(
            long availableMessageCount,
            long inFlightMessageCount,
            long delayedMessageCount
    ) {

        public static MainQueueResponse from(EmailOperationsQueueStatus.MainQueueSnapshot mainQueue) {
            if (mainQueue == null) {
                return null;
            }

            return new MainQueueResponse(
                    mainQueue.availableMessageCount(),
                    mainQueue.inFlightMessageCount(),
                    mainQueue.delayedMessageCount()
            );
        }
    }

    public record DeadLetterQueueResponse(
            long availableMessageCount
    ) {

        public static DeadLetterQueueResponse from(EmailOperationsQueueStatus.DeadLetterQueueSnapshot deadLetterQueue) {
            if (deadLetterQueue == null) {
                return null;
            }

            return new DeadLetterQueueResponse(deadLetterQueue.availableMessageCount());
        }
    }
}
