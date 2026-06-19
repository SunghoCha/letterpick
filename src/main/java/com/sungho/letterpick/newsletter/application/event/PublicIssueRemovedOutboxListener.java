package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRecordRequest;
import com.sungho.letterpick.common.outbox.OutboxMessageRecorder;
import com.sungho.letterpick.common.outbox.OutboxMessageType;
import com.sungho.letterpick.event.trending.PublicIssueRemovedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PublicIssueRemovedOutboxListener {

    private final OutboxMessageRecorder outboxMessageRecorder;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordOutboxMessage(PublicIssueRemovedEvent event) {
        outboxMessageRecorder.record(new OutboxMessageRecordRequest(
                event.eventId(),
                OutboxMessageType.PUBLIC_ISSUE_REMOVED,
                String.valueOf(event.issueId()),
                new PublicIssueRemovedPayload(event.issueId(), event.publicFeedCollectedAt()),
                event.occurredAt()
        ));
    }
}
