package com.sungho.letterpick.newsletter.adapter.mail;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(prefix = "letterpick.mail.sqs-listener", name = "enabled", havingValue = "true")
public class SesMailSqsListener {

    private final SesMailReceiveProcessor processor;

    public SesMailSqsListener(SesMailReceiveProcessor processor) {
        this.processor = requireNonNull(processor, "processor must not be null");
    }

    @SqsListener("${letterpick.mail.receive-queue}")
    public void receive(String sqsMessageBody) {
        processor.process(sqsMessageBody);
    }
}
