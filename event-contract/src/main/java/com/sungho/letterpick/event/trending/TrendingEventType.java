package com.sungho.letterpick.event.trending;

public enum TrendingEventType {

    PUBLIC_ISSUE_AVAILABLE("PUBLIC_ISSUE_AVAILABLE"),
    PUBLIC_ISSUE_REMOVED("PUBLIC_ISSUE_REMOVED"),
    ISSUE_VIEW_COUNT_UPDATED("ISSUE_VIEW_COUNT_UPDATED");

    private final String value;

    TrendingEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
