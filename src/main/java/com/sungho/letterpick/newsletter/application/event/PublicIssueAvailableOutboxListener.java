package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRecordRequest;
import com.sungho.letterpick.common.outbox.OutboxMessageRecorder;
import com.sungho.letterpick.common.outbox.OutboxMessageType;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PublicIssueAvailableOutboxListener {

    private final OutboxMessageRecorder outboxMessageRecorder;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordOutboxMessage(PublicIssueAvailableEvent event) {
        outboxMessageRecorder.record(new OutboxMessageRecordRequest(
                event.eventId(),
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE,
                String.valueOf(event.issueId()),
                new PublicIssueAvailablePayload(
                        event.issueId(),
                        event.newsletterId(),
                        event.category().name(),
                        event.publicFeedCollectedAt()
                ),
                event.publicFeedCollectedAt()
        ));
    }
}
