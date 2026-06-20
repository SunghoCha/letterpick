package com.sungho.letterpick.newsletter.adapter.trending;

import com.sungho.letterpick.newsletter.application.required.PublicIssueRankingReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
public class TrendingServiceClientConfiguration {

    @Bean
    RestClient trendingServiceRestClient(TrendingServiceProperties properties) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException("letterpick.trending-service.base-url must not be blank");
        }

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    PublicIssueRankingReader publicIssueRankingReader(RestClient trendingServiceRestClient) {
        return new RestClientPublicIssueRankingReader(trendingServiceRestClient);
    }
}
