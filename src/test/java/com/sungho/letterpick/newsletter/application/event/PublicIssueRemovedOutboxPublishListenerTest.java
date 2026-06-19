package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PublicIssueRemovedOutboxPublishListenerTest {

    @Mock
    private OutboxMessageRelay outboxMessageRelay;

    @InjectMocks
    private PublicIssueRemovedOutboxPublishListener listener;

    @Test
    @DisplayName("공개 이슈 삭제 이벤트의 eventId로 outbox 메시지 즉시 발행을 요청한다")
    void publishOutboxMessage() {
        PublicIssueRemovedEvent event = new PublicIssueRemovedEvent(
                "event-1",
                10L,
                Instant.parse("2050-06-10T00:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        listener.publishOutboxMessage(event);

        verify(outboxMessageRelay).publishByEventId("event-1");
    }

    @Test
    @DisplayName("outbox 즉시 발행 요청은 트랜잭션 커밋 후 수행한다")
    void publishOutboxMessageAfterCommit() throws Exception {
        Method method = PublicIssueRemovedOutboxPublishListener.class.getDeclaredMethod(
                "publishOutboxMessage",
                PublicIssueRemovedEvent.class
        );

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
