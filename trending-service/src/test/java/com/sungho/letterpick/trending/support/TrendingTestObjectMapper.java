package com.sungho.letterpick.trending.support;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TrendingTestObjectMapper {

    private TrendingTestObjectMapper() {
    }

    public static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
