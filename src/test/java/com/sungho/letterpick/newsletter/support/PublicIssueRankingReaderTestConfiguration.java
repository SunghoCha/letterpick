package com.sungho.letterpick.newsletter.support;

import com.sungho.letterpick.newsletter.application.required.PublicIssueRankingReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class PublicIssueRankingReaderTestConfiguration {

    @Bean
    PublicIssueRankingReader publicIssueRankingReader() {
        return (windowType, limit) -> List.of();
    }
}
