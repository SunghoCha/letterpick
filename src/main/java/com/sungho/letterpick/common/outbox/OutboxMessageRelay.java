package com.sungho.letterpick.common.outbox;

public interface OutboxMessageRelay {

    void publishByEventId(String eventId);

    int publishDueMessages(int limit);
}
