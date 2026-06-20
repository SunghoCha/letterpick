package com.sungho.letterpick.trending.ranking.adapter;

import com.sungho.letterpick.trending.ranking.adapter.persistence.PublicIssueRankingSummaryRepository;
import com.sungho.letterpick.trending.ranking.adapter.persistence.RdsSummaryPublicIssueRankingReader;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingReader;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class PublicIssueRankingReaderConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "letterpick.trending.ranking.summary",
            name = "reader",
            havingValue = "redis",
            matchIfMissing = true
    )
    PublicIssueRankingReader redisPublicIssueRankingReader(
            StringRedisTemplate redisTemplate,
            @Value("${letterpick.trending.ranking.summary.redis-key-prefix}") String rankingRedisKeyPrefix,
            @Value("${letterpick.trending.ranking.state.redis-key-prefix}") String issueStateRedisKeyPrefix,
            PublicIssueRankingLimitPolicy limitPolicy
    ) {
        return new RedisPublicIssueRankingReader(
                redisTemplate,
                rankingRedisKeyPrefix,
                issueStateRedisKeyPrefix,
                limitPolicy
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "letterpick.trending.ranking.summary",
            name = "reader",
            havingValue = "rds"
    )
    PublicIssueRankingReader rdsSummaryPublicIssueRankingReader(
            PublicIssueRankingSummaryRepository rankingSummaryRepository
    ) {
        return new RdsSummaryPublicIssueRankingReader(rankingSummaryRepository);
    }
}
