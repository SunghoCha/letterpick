package com.sungho.letterpick.trending.application;

public class TrendingMessageProcessingException extends RuntimeException {

    public TrendingMessageProcessingException(String message) {
        super(message);
    }

    public TrendingMessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
