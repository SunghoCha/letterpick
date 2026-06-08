package com.sungho.letterpick.trending.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.sungho.letterpick.event.EventEnvelope;

public interface TrendingEventHandler {

    String eventType();

    void handle(EventEnvelope<JsonNode> envelope);
}
