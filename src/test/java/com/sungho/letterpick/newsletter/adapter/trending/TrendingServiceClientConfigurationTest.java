package com.sungho.letterpick.newsletter.adapter.trending;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class TrendingServiceClientConfigurationTest {

    @Test
    @DisplayName("trending-service RestClient를 생성한다")
    void trending_service_rest_client() {
        // given
        TrendingServiceClientConfiguration configuration = new TrendingServiceClientConfiguration();
        TrendingServiceProperties properties = new TrendingServiceProperties("http://trending-service:8080");

        // when
        RestClient restClient = configuration.trendingServiceRestClient(properties);

        // then
        assertThat(restClient).isNotNull();
    }
}
