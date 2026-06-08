package com.sungho.letterpick.newsletter.adapter.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.sungho.letterpick.LetterPickRedisTestConfiguration;
import com.sungho.letterpick.newsletter.application.PublicIssueViewCountProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@DataRedisTest(properties = {
        "letterpick.public-issue.view-count.snapshot-interval=50",
        "letterpick.public-issue.view-count.dedupe-ttl=PT30M",
        "letterpick.public-issue.view-count.redis-key-prefix=letterpick:public-issue"
})
@EnableConfigurationProperties(PublicIssueViewCountProperties.class)
@Import({
        LetterPickRedisTestConfiguration.class,
        RedisPublicIssueViewCountStore.class
})
class RedisPublicIssueViewCountStoreTest {

    private static final String REDIS_KEY_PREFIX = "letterpick:public-issue";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisPublicIssueViewCountStore store;

    @BeforeEach
    void setUp() {
        redisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("같은 actor의 같은 이슈 조회는 dedupe TTL 동안 한 번만 허용한다")
    void incrementIfFirstView_allows_only_first_view_in_dedupe_window() {
        // when
        long first = store.incrementIfFirstView(10L, "member:20");
        long second = store.incrementIfFirstView(10L, "member:20");

        // then
        assertThat(first).isEqualTo(1L);
        assertThat(second).isZero();

        Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + ":{10}:view-dedupe:*");
        assertThat(keys).hasSize(1);
        String dedupeKey = keys.iterator().next();
        assertThat(dedupeKey).doesNotContain("member:20");
        assertThat(redisTemplate.getExpire(dedupeKey)).isPositive();
        assertThat(redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + ":{10}:view-count"))
                .isEqualTo("1");
    }

    @Test
    @DisplayName("유효 조회수 counter를 issueId별로 증가시킨다")
    void incrementIfFirstView_increments_counter_by_issue_id() {
        // when
        long first = store.incrementIfFirstView(10L, "member:20");
        long second = store.incrementIfFirstView(10L, "member:21");
        long otherIssue = store.incrementIfFirstView(11L, "member:20");

        // then
        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
        assertThat(otherIssue).isEqualTo(1L);
        assertThat(redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + ":{10}:view-count"))
                .isEqualTo("2");
        assertThat(redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + ":{11}:view-count"))
                .isEqualTo("1");
    }
}
