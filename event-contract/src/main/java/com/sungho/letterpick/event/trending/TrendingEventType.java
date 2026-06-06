package com.sungho.letterpick.event.trending;

public enum TrendingEventType {

    PUBLIC_ISSUE_AVAILABLE("PUBLIC_ISSUE_AVAILABLE");

    private final String value;

    TrendingEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
