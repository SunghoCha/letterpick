package com.sungho.letterpick.common.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxQueueNameResolverTest {

    @Test
    void rejectsNullQueueName() {
        assertThatThrownBy(() -> new OutboxQueueNameResolver(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");
    }

    @Test
    void rejectsBlankQueueName() {
        assertThatThrownBy(() -> new OutboxQueueNameResolver(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox queue name is not configured");
    }

    @Test
    void resolvesTrimmedQueueName() {
        OutboxQueueNameResolver resolver = new OutboxQueueNameResolver(" letterpick-test-trending-lifecycle-events ");

        assertThat(resolver.resolveQueueName(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE))
                .isEqualTo("letterpick-test-trending-lifecycle-events");
    }
}
