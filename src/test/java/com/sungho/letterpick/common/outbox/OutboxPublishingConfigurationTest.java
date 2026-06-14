package com.sungho.letterpick.common.outbox;

import io.awspring.cloud.sqs.operations.SqsOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxPublishingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OutboxMessageRepository.class, () -> mock(OutboxMessageRepository.class))
            .withBean(SqsOperations.class, () -> mock(SqsOperations.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(Clock.class, Clock::systemUTC)
            .withUserConfiguration(OutboxPublishingConfiguration.class);

    @Test
    @DisplayName("outbox 발행 설정이 켜져 있으면 SQS publisher, relay, retry worker를 등록한다")
    void registerOutboxPublisherAndRelayWhenPublishEnabled() {
        // when
        contextRunner
                .withPropertyValues("letterpick.outbox.publish.enabled=true")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(OutboxMessagePublisher.class);
                    assertThat(context).hasSingleBean(SqsOutboxMessagePublisher.class);
                    assertThat(context).hasSingleBean(OutboxMessageRelay.class);
                    assertThat(context).hasSingleBean(DefaultOutboxMessageRelay.class);
                    assertThat(context).hasSingleBean(OutboxMessageRetryWorker.class);
                });
    }

    @Test
    @DisplayName("outbox 발행 설정이 꺼져 있으면 SQS publisher와 relay를 등록하지 않는다")
    void doesNotRegisterOutboxPublisherAndRelayWhenPublishDisabled() {
        // when
        contextRunner.run(context -> {
            // then
            assertThat(context).doesNotHaveBean(OutboxMessagePublisher.class);
            assertThat(context).doesNotHaveBean(SqsOutboxMessagePublisher.class);
            assertThat(context).doesNotHaveBean(OutboxMessageRelay.class);
            assertThat(context).doesNotHaveBean(DefaultOutboxMessageRelay.class);
            assertThat(context).doesNotHaveBean(OutboxMessageRetryWorker.class);
        });
    }
}
