package com.sungho.letterpick.newsletter.application.required;

public class QueueStatusReadException extends RuntimeException {

    public QueueStatusReadException(String message) {
        super(message);
    }

    public QueueStatusReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
