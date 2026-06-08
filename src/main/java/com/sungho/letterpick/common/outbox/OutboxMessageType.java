package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.event.trending.TrendingEventType;

public enum OutboxMessageType {

    PUBLIC_ISSUE_AVAILABLE(
            TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
            1,
            "NEWSLETTER_ISSUE"
    ),
    ISSUE_VIEW_COUNT_UPDATED(
            TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
            1,
            "NEWSLETTER_ISSUE"
    );

    private final String eventType;
    private final int schemaVersion;
    private final String aggregateType;

    OutboxMessageType(String eventType, int schemaVersion, String aggregateType) {
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.aggregateType = aggregateType;
    }

    public String eventType() {
        return eventType;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String aggregateType() {
        return aggregateType;
    }
}
