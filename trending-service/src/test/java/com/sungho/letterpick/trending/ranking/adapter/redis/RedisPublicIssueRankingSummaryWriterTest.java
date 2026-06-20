package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.TrendingRedisTestConfiguration;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest(properties = {
        "letterpick.trending.ranking.summary.writer=redis",
        "letterpick.trending.ranking.summary.redis-key-prefix=letterpick:trending:ranking"
})
@Import({
        TrendingRedisTestConfiguration.class,
        PublicIssueRankingLimitPolicy.class,
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
        assertThat(rankingKey)
                .isEqualTo("letterpick:trending:ranking:{DAILY:2050-06-12}:issues");
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
    @DisplayName("window ranking은 score 상위 100개만 유지한다")
    void keep_top_100_ranked_issues() {
        // given
        PublicIssueRankingWindow window = dailyWindow();

        // when
        for (long issueId = 1; issueId <= 101; issueId++) {
            writer.save(window, issueId, issueId, CALCULATED_AT);
        }

        // then
        String rankingKey = rankingKey(window);
        assertThat(redisTemplate.opsForZSet().size(rankingKey))
                .isEqualTo(100L);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "1"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "2"))
                .isEqualTo(2.0);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "101"))
                .isEqualTo(101.0);
    }

    @Test
    @DisplayName("window ranking 유지 개수는 설정된 maxSize를 따른다")
    void keep_configured_max_size_ranked_issues() {
        // given
        PublicIssueRankingWindow window = dailyWindow();
        RedisPublicIssueRankingSummaryWriter maxSizeThreeWriter = new RedisPublicIssueRankingSummaryWriter(
                redisTemplate,
                redisKeyPrefix,
                new PublicIssueRankingLimitPolicy(2, 3)
        );

        // when
        for (long issueId = 1; issueId <= 4; issueId++) {
            maxSizeThreeWriter.save(window, issueId, issueId, CALCULATED_AT);
        }

        // then
        String rankingKey = rankingKey(window);
        assertThat(redisTemplate.opsForZSet().size(rankingKey))
                .isEqualTo(3L);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "1"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "2"))
                .isEqualTo(2.0);
        assertThat(redisTemplate.opsForZSet().score(rankingKey, "4"))
                .isEqualTo(4.0);
    }

    @Test
    @DisplayName("daily ranking은 window 종료 후 2일 뒤 만료된다")
    void daily_ranking_expires_after_two_days_from_window_end() {
        // given
        PublicIssueRankingWindow window = PublicIssueRankingWindow.daily(
                LocalDate.now(RANKING_ZONE),
                RANKING_ZONE
        );

        // when
        writer.save(window, 10L, 150L, Instant.now());

        // then
        assertThat(redisTemplate.getExpire(rankingKey(window)))
                .isBetween(2 * 24 * 60 * 60L, 3 * 24 * 60 * 60L + 60L);
    }

    @Test
    @DisplayName("weekly ranking은 window 종료 후 7일 뒤 만료된다")
    void weekly_ranking_expires_after_seven_days_from_window_end() {
        // given
        PublicIssueRankingWindow window = PublicIssueRankingWindow.weekly(
                LocalDate.now(RANKING_ZONE),
                RANKING_ZONE
        );

        // when
        writer.save(window, 10L, 150L, Instant.now());

        // then
        assertThat(redisTemplate.getExpire(rankingKey(window)))
                .isBetween(7 * 24 * 60 * 60L, 14 * 24 * 60 * 60L + 60L);
    }

    @Test
    @DisplayName("window 기준으로 저장된 ranking에서 issueId를 제거한다")
    void delete_summary_by_window() {
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
        writer.delete(daily, 10L);

        // then
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(weekly), "10"))
                .isEqualTo(150.0);
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "11"))
                .isEqualTo(90.0);
    }

    @Test
    @DisplayName("blank Redis key prefix는 허용하지 않는다")
    void reject_blank_redis_key_prefix() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingSummaryWriter(
                redisTemplate,
                " ",
                new PublicIssueRankingLimitPolicy(20, 100)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not be blank");
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 Redis key prefix는 허용하지 않는다")
    void reject_redis_key_prefix_with_surrounding_whitespace() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingSummaryWriter(
                redisTemplate,
                " letterpick:trending:ranking",
                new PublicIssueRankingLimitPolicy(20, 100)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not contain leading or trailing whitespace");
    }

    private PublicIssueRankingWindow dailyWindow() {
        return PublicIssueRankingWindow.daily(LocalDate.of(2050, 6, 12), RANKING_ZONE);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        String windowHashTag = window.type().name() + ":" + window.key();
        return String.join(":", redisKeyPrefix, "{" + windowHashTag + "}", "issues");
    }
}
