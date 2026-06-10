package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRecordRequest;
import com.sungho.letterpick.common.outbox.OutboxMessageRecorder;
import com.sungho.letterpick.common.outbox.OutboxMessageType;
import com.sungho.letterpick.event.trending.PublicIssueRemovedPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicIssueRemovedOutboxListenerTest {

    @Mock
    private OutboxMessageRecorder outboxMessageRecorder;

    @InjectMocks
    private PublicIssueRemovedOutboxListener listener;

    @Test
    @DisplayName("공개 이슈 삭제 이벤트를 outbox 메시지 저장 요청으로 변환한다")
    void recordOutboxMessage() {
        PublicIssueRemovedEvent event = new PublicIssueRemovedEvent(
                "event-1",
                10L,
                Instant.parse("2050-06-10T01:00:00Z")
        );

        listener.recordOutboxMessage(event);

        ArgumentCaptor<OutboxMessageRecordRequest> requestCaptor = ArgumentCaptor.forClass(OutboxMessageRecordRequest.class);
        verify(outboxMessageRecorder).record(requestCaptor.capture());

        OutboxMessageRecordRequest request = requestCaptor.getValue();
        assertThat(request.eventId()).isEqualTo("event-1");
        assertThat(request.type()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_REMOVED);
        assertThat(request.aggregateId()).isEqualTo("10");
        assertThat(request.occurredAt()).isEqualTo(event.occurredAt());

        assertThat(request.payload()).isInstanceOf(PublicIssueRemovedPayload.class);
        PublicIssueRemovedPayload payload = (PublicIssueRemovedPayload) request.payload();
        assertThat(payload.issueId()).isEqualTo(event.issueId());
    }

    @Test
    @DisplayName("outbox 기록은 트랜잭션 커밋 전에 수행한다")
    void recordOutboxMessageBeforeCommit() throws Exception {
        Method method = PublicIssueRemovedOutboxListener.class.getDeclaredMethod(
                "recordOutboxMessage",
                PublicIssueRemovedEvent.class
        );

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }
}
