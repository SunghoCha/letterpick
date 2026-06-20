package com.sungho.letterpick.newsletter.adapter.trending;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "letterpick.trending-service")
public record TrendingServiceProperties(
        String baseUrl
) {
}
