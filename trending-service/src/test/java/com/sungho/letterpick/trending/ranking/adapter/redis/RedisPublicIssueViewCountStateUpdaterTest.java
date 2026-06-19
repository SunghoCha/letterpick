package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.TrendingRedisTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest(properties = {
        "letterpick.trending.ranking.state.redis-key-prefix=letterpick:trending:issue"
})
@Import({
        TrendingRedisTestConfiguration.class,
        RedisPublicIssueViewCountStateUpdater.class
})
class RedisPublicIssueViewCountStateUpdaterTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisPublicIssueViewCountStateUpdater updater;

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
    @DisplayName("조회수 state가 없으면 새 조회수를 저장한다")
    void save_view_count_when_absent_and_available() {
        // given
        markAvailable(10L);

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, 150L);

        // then
        assertThat(accepted).isTrue();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isEqualTo("150");
    }

    @Test
    @DisplayName("더 큰 조회수는 issue state에 반영한다")
    void update_greater_view_count() {
        // given
        markAvailable(10L);
        markViewCount(10L, "150");

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, 200L);

        // then
        assertThat(accepted).isTrue();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isEqualTo("200");
    }

    @Test
    @DisplayName("같은 조회수는 stale 이벤트로 보지 않는다")
    void accept_same_view_count() {
        // given
        markAvailable(10L);
        markViewCount(10L, "200");

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, 200L);

        // then
        assertThat(accepted).isTrue();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isEqualTo("200");
    }

    @Test
    @DisplayName("늦게 도착한 낮은 조회수는 issue state를 낮추지 않는다")
    void ignore_lower_view_count() {
        // given
        markAvailable(10L);
        markViewCount(10L, "200");

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, 150L);

        // then
        assertThat(accepted).isFalse();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isEqualTo("200");
    }

    @Test
    @DisplayName("음수 조회수는 issue state에 반영하지 않는다")
    void reject_negative_view_count() {
        // given
        markAvailable(10L);
        markViewCount(10L, "200");

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, -1L);

        // then
        assertThat(accepted).isFalse();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isEqualTo("200");
    }

    @Test
    @DisplayName("공개 상태가 아니면 조회수를 갱신하지 않는다")
    void skip_when_issue_is_not_available() {
        // given
        redisTemplate.opsForHash().putAll(issueStateKey(10L), Map.of(
                "status", "REMOVED"
        ));

        // when
        boolean accepted = updater.acceptIfAvailableAndNotStale(10L, 150L);

        // then
        assertThat(accepted).isFalse();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "view_count"))
                .isNull();
    }

    private void markAvailable(Long issueId) {
        redisTemplate.opsForHash().putAll(issueStateKey(issueId), Map.of(
                "status", "AVAILABLE"
        ));
    }

    private void markViewCount(Long issueId, String viewCount) {
        redisTemplate.opsForHash().putAll(issueStateKey(issueId), Map.of(
                "view_count", viewCount
        ));
    }

    private String issueStateKey(Long issueId) {
        return String.join(":", issueStateRedisKeyPrefix, "{" + issueId + "}", "state");
    }
}
