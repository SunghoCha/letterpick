package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.TrendingRedisTestConfiguration;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowCalculator;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest(properties = {
        "letterpick.trending.ranking.summary.writer=redis",
        "letterpick.trending.ranking.summary.redis-key-prefix=letterpick:trending:ranking",
        "letterpick.trending.ranking.state.redis-key-prefix=letterpick:trending:issue"
})
@Import({
        TrendingRedisTestConfiguration.class,
        PublicIssueRankingWindowCalculator.class,
        RedisPublicIssueRankingStateWriter.class
})
class RedisPublicIssueRankingStateWriterTest {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant COLLECTED_AT = Instant.parse("2050-06-12T01:00:00Z");

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PublicIssueRankingWindowCalculator windowCalculator;

    @Autowired
    private RedisPublicIssueRankingStateWriter writer;

    @Value("${letterpick.trending.ranking.summary.redis-key-prefix}")
    private String rankingRedisKeyPrefix;

    @Value("${letterpick.trending.ranking.state.redis-key-prefix}")
    private String issueStateRedisKeyPrefix;

    @BeforeEach
    void setUp() {
        redisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("공개 가능 상태를 issue state Hash에 저장한다")
    void mark_available_issue_state() {
        // when
        writer.markAvailable(10L, COLLECTED_AT);

        // then
        String issueStateKey = issueStateKey(10L);
        assertThat(redisTemplate.opsForHash().entries(issueStateKey))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "status", "AVAILABLE",
                        "collected_at", COLLECTED_AT.toString()
                ));
        assertThat(redisTemplate.getExpire(issueStateKey))
                .isPositive();
    }

    @Test
    @DisplayName("삭제 상태를 저장하고 daily/weekly ranking에서 issueId를 제거한다")
    void mark_removed_issue_state_and_remove_rankings() {
        // given
        PublicIssueRankingWindow daily = dailyWindow();
        PublicIssueRankingWindow weekly = weeklyWindow();
        redisTemplate.opsForZSet().add(rankingKey(daily), "10", 150.0);
        redisTemplate.opsForZSet().add(rankingKey(weekly), "10", 150.0);
        redisTemplate.opsForZSet().add(rankingKey(daily), "11", 90.0);

        // when
        writer.markRemoved(10L, COLLECTED_AT);

        // then
        String issueStateKey = issueStateKey(10L);
        assertThat(redisTemplate.opsForHash().entries(issueStateKey))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "status", "REMOVED",
                        "collected_at", COLLECTED_AT.toString()
                ));
        assertThat(redisTemplate.getExpire(issueStateKey))
                .isPositive();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(weekly), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(rankingKey(daily), "11"))
                .isEqualTo(90.0);
    }

    @Test
    @DisplayName("REMOVED 상태는 AVAILABLE 이벤트로 되살리지 않는다")
    void removed_state_is_terminal() {
        // given
        writer.markRemoved(10L, COLLECTED_AT);

        // when
        writer.markAvailable(10L, COLLECTED_AT.plusSeconds(10));

        // then
        assertThat(redisTemplate.opsForHash().entries(issueStateKey(10L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "status", "REMOVED",
                        "collected_at", COLLECTED_AT.toString()
                ));
    }

    @Test
    @DisplayName("blank issue state Redis key prefix는 허용하지 않는다")
    void reject_blank_issue_state_redis_key_prefix() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingStateWriter(
                redisTemplate,
                windowCalculator,
                " ",
                rankingRedisKeyPrefix
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not be blank");
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 issue state Redis key prefix는 허용하지 않는다")
    void reject_issue_state_redis_key_prefix_with_surrounding_whitespace() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingStateWriter(
                redisTemplate,
                windowCalculator,
                " letterpick:trending:issue",
                rankingRedisKeyPrefix
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not contain leading or trailing whitespace");
    }

    @Test
    @DisplayName("blank ranking Redis key prefix는 허용하지 않는다")
    void reject_blank_ranking_redis_key_prefix() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingStateWriter(
                redisTemplate,
                windowCalculator,
                issueStateRedisKeyPrefix,
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not be blank");
    }

    private PublicIssueRankingWindow dailyWindow() {
        return PublicIssueRankingWindow.daily(LocalDate.of(2050, 6, 12), RANKING_ZONE);
    }

    private PublicIssueRankingWindow weeklyWindow() {
        return PublicIssueRankingWindow.weekly(LocalDate.of(2050, 6, 12), RANKING_ZONE);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        String windowHashTag = window.type().name() + ":" + window.key();
        return String.join(":", rankingRedisKeyPrefix, "{" + windowHashTag + "}", "issues");
    }

    private String issueStateKey(Long issueId) {
        return String.join(":", issueStateRedisKeyPrefix, "{" + issueId + "}", "state");
    }
}
