package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.TrendingRedisTestConfiguration;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest(properties = {
        "letterpick.trending.ranking.summary.writer=redis",
        "letterpick.trending.ranking.summary.redis-key-prefix=letterpick:trending:ranking"
})
@Import({
        TrendingRedisTestConfiguration.class,
        RedisPublicIssueRankingSummaryWriter.class
})
class RedisPublicIssueRankingSummaryWriterTest {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant CALCULATED_AT = Instant.parse("2050-06-12T01:00:00Z");

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisPublicIssueRankingSummaryWriter writer;

    @Value("${letterpick.trending.ranking.summary.redis-key-prefix}")
    private String redisKeyPrefix;

    @BeforeEach
    void setUp() {
        redisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("window별 ZSET에 issueId와 score를 저장한다")
    void save_summary_to_window_zset() {
        // given
        PublicIssueRankingWindow window = dailyWindow();

        // when
        writer.save(window, 10L, 150L, CALCULATED_AT);

        // then
        String rankingKey = rankingKey(window);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "10"))
                .isEqualTo(150.0);
        assertThat(redisTemplate.opsForSet().members(issueRankingKeysKey(10L)))
                .containsExactly(rankingKey);
    }

    @Test
    @DisplayName("같은 window와 issueId 저장은 score를 덮어쓴다")
    void save_summary_updates_existing_score() {
        // given
        PublicIssueRankingWindow window = dailyWindow();

        // when
        writer.save(window, 10L, 150L, CALCULATED_AT);
        writer.save(window, 10L, 220L, CALCULATED_AT);

        // then
        String rankingKey = rankingKey(window);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "10"))
                .isEqualTo(220.0);
        assertThat(redisTemplate.opsForZSet().size(rankingKey))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("issueId 기준으로 저장된 모든 window ranking에서 제거한다")
    void delete_summary_by_issue_id() {
        // given
        PublicIssueRankingWindow daily = dailyWindow();
        PublicIssueRankingWindow weekly = PublicIssueRankingWindow.weekly(
                LocalDate.of(2050, 6, 12),
                RANKING_ZONE
        );
        writer.save(daily, 10L, 150L, CALCULATED_AT);
        writer.save(weekly, 10L, 150L, CALCULATED_AT);
        writer.save(daily, 11L, 90L, CALCULATED_AT);

        // when
        writer.deleteByIssueId(10L);

        // then
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(weekly), "10"))
                .isNull();
        assertThat(redisTemplate.hasKey(issueRankingKeysKey(10L)))
                .isFalse();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "11"))
                .isEqualTo(90.0);
    }

    private PublicIssueRankingWindow dailyWindow() {
        return PublicIssueRankingWindow.daily(LocalDate.of(2050, 6, 12), RANKING_ZONE);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        return String.join(":", redisKeyPrefix, window.type().name(), window.key());
    }

    private String issueRankingKeysKey(Long issueId) {
        return String.join(":", redisKeyPrefix, "issue", "{" + issueId + "}", "ranking-keys");
    }
}
