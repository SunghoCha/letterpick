package com.sungho.letterpick.common.outbox;

public class OutboxPayloadSerializationException extends RuntimeException {

    public OutboxPayloadSerializationException(Throwable cause) {
        super("Failed to serialize outbox payload", cause);
    }
}
