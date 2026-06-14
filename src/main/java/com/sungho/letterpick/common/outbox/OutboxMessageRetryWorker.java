package com.sungho.letterpick.common.outbox;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class OutboxMessageRetryWorker {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final OutboxMessageRelay outboxMessageRelay;

    @Scheduled(fixedDelayString = "${letterpick.outbox.retry.fixed-delay:PT30S}")
    @SchedulerLock(
            name = "outboxMessageRetryWorker",
            lockAtMostFor = "${letterpick.outbox.retry.lock-at-most-for:PT1M}",
            lockAtLeastFor = "${letterpick.outbox.retry.lock-at-least-for:PT0S}"
    )
    public int publishDueOutboxMessages() {
        return outboxMessageRelay.publishDueMessages(DEFAULT_BATCH_SIZE);
    }
}
