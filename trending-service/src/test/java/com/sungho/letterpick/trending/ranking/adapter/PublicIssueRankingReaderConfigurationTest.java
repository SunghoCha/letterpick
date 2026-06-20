package com.sungho.letterpick.trending.ranking.adapter;

import com.sungho.letterpick.trending.ranking.adapter.persistence.PublicIssueRankingSummaryRepository;
import com.sungho.letterpick.trending.ranking.adapter.persistence.RdsSummaryPublicIssueRankingReader;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingReader;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PublicIssueRankingReaderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(PublicIssueRankingLimitPolicy.class, () -> new PublicIssueRankingLimitPolicy(20, 100))
            .withBean(PublicIssueRankingSummaryRepository.class, () -> mock(PublicIssueRankingSummaryRepository.class))
            .withUserConfiguration(PublicIssueRankingReaderConfiguration.class);

    @Test
    @DisplayName("Redis reader 설정이면 Redis ranking reader를 등록한다")
    void register_redis_reader_when_reader_is_redis() {
        contextRunner
                .withPropertyValues(
                        "letterpick.trending.ranking.summary.reader=redis",
                        "letterpick.trending.ranking.summary.redis-key-prefix=letterpick:trending:ranking",
                        "letterpick.trending.ranking.state.redis-key-prefix=letterpick:trending:issue"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicIssueRankingReader.class);
                    assertThat(context).hasSingleBean(RedisPublicIssueRankingReader.class);
                    assertThat(context).doesNotHaveBean(RdsSummaryPublicIssueRankingReader.class);
                });
    }

    @Test
    @DisplayName("RDS reader 설정이면 RDS summary ranking reader를 등록한다")
    void register_rds_reader_when_reader_is_rds() {
        contextRunner
                .withPropertyValues("letterpick.trending.ranking.summary.reader=rds")
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicIssueRankingReader.class);
                    assertThat(context).hasSingleBean(RdsSummaryPublicIssueRankingReader.class);
                    assertThat(context).doesNotHaveBean(RedisPublicIssueRankingReader.class);
                });
    }
}
