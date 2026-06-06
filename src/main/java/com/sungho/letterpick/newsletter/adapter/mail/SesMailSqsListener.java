package com.sungho.letterpick.newsletter.adapter.mail;

import com.sungho.letterpick.common.logging.MdcInterceptor;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
        String previousRequestId = MDC.get(MdcInterceptor.REQUEST_ID);
        MDC.put(MdcInterceptor.REQUEST_ID, UUID.randomUUID().toString());
        try {
            processor.process(sqsMessageBody);
        } finally {
            restoreRequestId(previousRequestId);
        }
    }

    private void restoreRequestId(String previousRequestId) {
        if (previousRequestId == null) {
            MDC.remove(MdcInterceptor.REQUEST_ID);
            return;
        }
        MDC.put(MdcInterceptor.REQUEST_ID, previousRequestId);
    }
}
