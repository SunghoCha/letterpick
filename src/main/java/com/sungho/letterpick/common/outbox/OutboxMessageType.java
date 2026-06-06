package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.event.trending.TrendingEventType;

public enum OutboxMessageType {

    PUBLIC_ISSUE_AVAILABLE(
            TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
            "TRENDING_LIFECYCLE_EVENTS",
            1,
            "NEWSLETTER_ISSUE"
    );

    private final String eventType;
    private final String destination;
    private final int schemaVersion;
    private final String aggregateType;

    OutboxMessageType(String eventType, String destination, int schemaVersion, String aggregateType) {
        this.eventType = eventType;
        this.destination = destination;
        this.schemaVersion = schemaVersion;
        this.aggregateType = aggregateType;
    }

    public String eventType() {
        return eventType;
    }

    public String destination() {
        return destination;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String aggregateType() {
        return aggregateType;
    }
}
