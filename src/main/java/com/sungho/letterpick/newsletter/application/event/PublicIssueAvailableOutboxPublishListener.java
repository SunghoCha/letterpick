package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRelay;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
public class PublicIssueAvailableOutboxPublishListener {

    private final OutboxMessageRelay outboxMessageRelay;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOutboxMessage(PublicIssueAvailableEvent event) {
        outboxMessageRelay.publishByEventId(event.eventId());
    }
}
