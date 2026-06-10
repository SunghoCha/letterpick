package com.sungho.letterpick.trending.lifecycle.adapter.sqs;

import com.sungho.letterpick.trending.application.TrendingMessageProcessor;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(prefix = "letterpick.trending.sqs-listener", name = "enabled", havingValue = "true")
public class TrendingLifecycleSqsListener {

    private static final String QUEUE_NAME_PROPERTY = "letterpick.trending.lifecycle-events-queue";

    private final TrendingMessageProcessor processor;
    private final String queueName;

    public TrendingLifecycleSqsListener(
            TrendingMessageProcessor processor,
            @Value("${" + QUEUE_NAME_PROPERTY + "}") String queueName
    ) {
        this.processor = requireNonNull(processor, "processor must not be null");
        if (queueName == null || queueName.isBlank() || !queueName.equals(queueName.trim())) {
            throw new IllegalStateException("trending lifecycle queue name is not configured: "
                    + QUEUE_NAME_PROPERTY);
        }
        this.queueName = queueName;
    }

    @SqsListener("${letterpick.trending.lifecycle-events-queue}")
    public void receive(String messageBody) {
        processor.process(messageBody, queueName);
    }
}
