package com.sungho.letterpick.common.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxMessageRetryWorkerTest {

    @Mock
    private OutboxMessageRelay outboxMessageRelay;

    @InjectMocks
    private OutboxMessageRetryWorker worker;

    @Test
    @DisplayName("재발행 대상 outbox 메시지를 기본 배치 크기만큼 발행 요청한다")
    void publishDueOutboxMessages() {
        given(outboxMessageRelay.publishDueMessages(100))
                .willReturn(3);

        int published = worker.publishDueOutboxMessages();

        assertThat(published).isEqualTo(3);
        verify(outboxMessageRelay).publishDueMessages(100);
    }
}
