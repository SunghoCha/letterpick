package com.sungho.letterpick.trending.score.application;

import com.sungho.letterpick.event.EventEnvelope;
import tools.jackson.databind.JsonNode;

public interface TrendingScoreEventHandler {

    String eventType();

    void handle(EventEnvelope<JsonNode> envelope);
}
