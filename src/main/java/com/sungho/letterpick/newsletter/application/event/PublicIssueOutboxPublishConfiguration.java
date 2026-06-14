package com.sungho.letterpick.newsletter.application.event;

import com.sungho.letterpick.common.outbox.OutboxMessageRelay;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "letterpick.outbox.publish", name = "enabled", havingValue = "true")
public class PublicIssueOutboxPublishConfiguration {

    @Bean
    PublicIssueAvailableOutboxPublishListener publicIssueAvailableOutboxPublishListener(
            OutboxMessageRelay outboxMessageRelay
    ) {
        return new PublicIssueAvailableOutboxPublishListener(outboxMessageRelay);
    }

    @Bean
    PublicIssueRemovedOutboxPublishListener publicIssueRemovedOutboxPublishListener(
            OutboxMessageRelay outboxMessageRelay
    ) {
        return new PublicIssueRemovedOutboxPublishListener(outboxMessageRelay);
    }
}
