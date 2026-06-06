package com.sungho.letterpick.common.outbox;

public interface OutboxMessagePublisher {

    void publish(OutboxMessage message);
}
