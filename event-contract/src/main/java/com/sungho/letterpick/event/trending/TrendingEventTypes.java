package com.sungho.letterpick.event.trending;

public enum TrendingEventTypes {

    PUBLIC_ISSUE_AVAILABLE("PUBLIC_ISSUE_AVAILABLE");

    private final String value;

    TrendingEventTypes(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
