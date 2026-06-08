package com.sungho.letterpick.trending.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;

@Configuration
public class TrendingJsonConfig {

    @Bean
    JsonMapperBuilderCustomizer trendingJsonMapperBuilderCustomizer() {
        return builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
