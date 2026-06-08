package com.sungho.letterpick.trending.application;

import com.sungho.letterpick.event.EventEnvelope;
import tools.jackson.databind.JsonNode;

public interface TrendingEventProcessingService {

    void process(EventEnvelope<JsonNode> envelope);
}
