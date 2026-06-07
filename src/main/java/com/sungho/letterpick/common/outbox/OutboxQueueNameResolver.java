package com.sungho.letterpick.common.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OutboxQueueNameResolver {

    private final Map<OutboxMessageType, String> queueNames;

    public OutboxQueueNameResolver(
            @Value("${letterpick.outbox.queue.trending-lifecycle-events:trending-lifecycle-events}")
            String trendingLifecycleEventsQueue
    ) {
        this.queueNames = Map.of(
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE, trendingLifecycleEventsQueue
        );
    }

    public String resolveQueueName(OutboxMessageType type) {
        String queueName = queueNames.get(type);
        if (queueName == null || queueName.isBlank()) {
            throw new IllegalArgumentException("outbox queue name is not configured: " + type);
        }
        return queueName;
    }
}
