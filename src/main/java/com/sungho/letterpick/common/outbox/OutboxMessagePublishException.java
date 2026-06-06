package com.sungho.letterpick.common.outbox;

public class OutboxMessagePublishException extends RuntimeException {

    public OutboxMessagePublishException(Throwable cause) {
        super("Failed to publish outbox message", cause);
    }
}
