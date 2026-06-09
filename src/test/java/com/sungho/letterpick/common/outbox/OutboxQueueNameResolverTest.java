package com.sungho.letterpick.common.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxQueueNameResolverTest {

    @Test
    @DisplayName("queue name 설정이 null이면 생성 시점에 실패한다")
    void rejectsNullQueueName() {
        assertThatThrownBy(() -> new OutboxQueueNameResolver(
                null,
                "letterpick-test-trending-score-events"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");

        assertThatThrownBy(() -> new OutboxQueueNameResolver(
                "letterpick-test-trending-lifecycle-events",
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");
    }

    @Test
    @DisplayName("queue name 설정이 blank이면 생성 시점에 실패한다")
    void rejectsBlankQueueName() {
        assertThatThrownBy(() -> new OutboxQueueNameResolver(
                " ",
                "letterpick-test-trending-score-events"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");

        assertThatThrownBy(() -> new OutboxQueueNameResolver(
                "letterpick-test-trending-lifecycle-events",
                " "
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");
    }

    @Test
    @DisplayName("queue name 앞뒤 공백을 제거하고 이벤트 타입별 queue name을 반환한다")
    void resolvesTrimmedQueueName() {
        OutboxQueueNameResolver resolver = new OutboxQueueNameResolver(
                " letterpick-test-trending-lifecycle-events ",
                " letterpick-test-trending-score-events "
        );

        assertThat(resolver.resolveQueueName(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE))
                .isEqualTo("letterpick-test-trending-lifecycle-events");
        assertThat(resolver.resolveQueueName(OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED))
                .isEqualTo("letterpick-test-trending-score-events");
    }
}
