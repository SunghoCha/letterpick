package com.sungho.letterpick.trending.score.adapter.sqs;

import com.sungho.letterpick.trending.application.TrendingMessageProcessor;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(prefix = "letterpick.trending.sqs-listener", name = "enabled", havingValue = "true")
public class TrendingScoreSqsListener {

    private static final String QUEUE_NAME_PROPERTY = "letterpick.trending.score-events-queue";
    private static final String MAX_CONCURRENT_MESSAGES_PROPERTY =
            "letterpick.trending.sqs-listener.score.max-concurrent-messages";

    private final TrendingMessageProcessor processor;
    private final String queueName;

    public TrendingScoreSqsListener(
            TrendingMessageProcessor processor,
            @Value("${" + QUEUE_NAME_PROPERTY + "}") String queueName
    ) {
        this.processor = requireNonNull(processor, "processor must not be null");
        if (queueName == null || queueName.isBlank() || !queueName.equals(queueName.trim())) {
            throw new IllegalStateException("trending score queue name is not configured: "
                    + QUEUE_NAME_PROPERTY);
        }
        this.queueName = queueName;
    }

    @SqsListener(
            value = "${" + QUEUE_NAME_PROPERTY + "}",
            maxConcurrentMessages = "${" + MAX_CONCURRENT_MESSAGES_PROPERTY + "}"
    )
    @WithSpan("trending.score.consume")
    public void receive(String messageBody) {
        processor.process(messageBody, queueName);
    }
}
