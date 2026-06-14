package com.sungho.letterpick.common.outbox;

import io.awspring.cloud.sqs.operations.SqsOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "letterpick.outbox.publish", name = "enabled", havingValue = "true")
public class OutboxPublishingConfiguration {

    @Bean
    SqsOutboxMessagePublisher outboxMessagePublisher(SqsOperations sqsOperations, ObjectMapper objectMapper) {
        return new SqsOutboxMessagePublisher(sqsOperations, objectMapper);
    }

    @Bean
    DefaultOutboxMessageRelay outboxMessageRelay(OutboxMessageRepository outboxMessageRepository,
                                                 OutboxMessagePublisher outboxMessagePublisher,
                                                 Clock clock) {
        return new DefaultOutboxMessageRelay(outboxMessageRepository, outboxMessagePublisher, clock);
    }

    @Bean
    OutboxMessageRetryWorker outboxMessageRetryWorker(OutboxMessageRelay outboxMessageRelay) {
        return new OutboxMessageRetryWorker(outboxMessageRelay);
    }
}
