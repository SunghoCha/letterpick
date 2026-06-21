package com.sungho.letterpick.newsletter.adapter.trending;

import com.sungho.letterpick.newsletter.application.required.PublicIssueRankingReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
public class TrendingServiceClientConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    RestClient trendingServiceRestClient(TrendingServiceProperties properties) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException("letterpick.trending-service.base-url must not be blank");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    PublicIssueRankingReader publicIssueRankingReader(RestClient trendingServiceRestClient) {
        return new RestClientPublicIssueRankingReader(trendingServiceRestClient);
    }
}
