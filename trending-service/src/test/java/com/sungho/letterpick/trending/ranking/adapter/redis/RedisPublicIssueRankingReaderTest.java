package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.TrendingRedisTestConfiguration;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataRedisTest(properties = {
        "letterpick.trending.ranking.summary.reader=redis",
        "letterpick.trending.ranking.summary.redis-key-prefix=letterpick:trending:ranking",
        "letterpick.trending.ranking.state.redis-key-prefix=letterpick:trending:issue"
})
@Import(TrendingRedisTestConfiguration.class)
class RedisPublicIssueRankingReaderTest {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisPublicIssueRankingReader reader;

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
        reader = new RedisPublicIssueRankingReader(
                redisTemplate,
                rankingRedisKeyPrefix,
                issueStateRedisKeyPrefix,
                new PublicIssueRankingLimitPolicy(20, 100)
        );
    }

    @Test
    @DisplayName("window ZSET에서 score 높은 순으로 ranking을 조회한다")
    void find_top_rankings_from_window_zset() {
        // given
        PublicIssueRankingWindow window = dailyWindow();
        redisTemplate.opsForZSet().add(rankingKey(window), "10", 120);
        redisTemplate.opsForZSet().add(rankingKey(window), "40", 999);
        redisTemplate.opsForZSet().add(rankingKey(window), "60", 80);
        redisTemplate.opsForZSet().add(rankingKey(window), "90", 2_000);
        markAvailable(10L);
        markAvailable(40L);
        markAvailable(60L);
        markAvailable(90L);

        // when
        List<PublicIssueRankingItem> rankingItems = reader.findTop(window, 3);

        // then
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::issueId)
                .containsExactly(90L, 40L, 10L);
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::score)
                .containsExactly(2_000L, 999L, 120L);
    }

    @Test
    @DisplayName("REMOVED 또는 state가 없는 issue는 ranking 응답에서 제외하고 다음 AVAILABLE issue로 채운다")
    void filter_unavailable_issue_state_from_rankings() {
        // given
        PublicIssueRankingWindow window = dailyWindow();
        redisTemplate.opsForZSet().add(rankingKey(window), "10", 1_000);
        redisTemplate.opsForZSet().add(rankingKey(window), "20", 900);
        redisTemplate.opsForZSet().add(rankingKey(window), "30", 800);
        redisTemplate.opsForZSet().add(rankingKey(window), "40", 700);
        markRemoved(10L);
        markAvailable(30L);
        markAvailable(40L);

        // when
        List<PublicIssueRankingItem> rankingItems = reader.findTop(window, 2);

        // then
        assertThat(rankingItems)
                .extracting(PublicIssueRankingItem::issueId)
                .containsExactly(30L, 40L);
    }

    @Test
    @DisplayName("window ZSET이 없으면 빈 목록을 반환한다")
    void return_empty_when_window_zset_does_not_exist() {
        assertThat(reader.findTop(dailyWindow(), 10))
                .isEmpty();
    }

    @Test
    @DisplayName("blank Redis key prefix는 허용하지 않는다")
    void reject_blank_redis_key_prefix() {
        assertThatThrownBy(() -> new RedisPublicIssueRankingReader(
                redisTemplate,
                " ",
                issueStateRedisKeyPrefix,
                new PublicIssueRankingLimitPolicy(20, 100)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redisKeyPrefix must not be blank");
    }

    private PublicIssueRankingWindow dailyWindow() {
        return PublicIssueRankingWindow.daily(LocalDate.of(2050, 6, 12), RANKING_ZONE);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        String windowHashTag = window.type().name() + ":" + window.key();
        return String.join(":", rankingRedisKeyPrefix, "{" + windowHashTag + "}", "issues");
    }

    private void markAvailable(Long issueId) {
        markStatus(issueId, PublicIssueCandidateStatus.AVAILABLE);
    }

    private void markRemoved(Long issueId) {
        markStatus(issueId, PublicIssueCandidateStatus.REMOVED);
    }

    private void markStatus(Long issueId, PublicIssueCandidateStatus status) {
        redisTemplate.opsForHash().put(
                issueStateKey(issueId),
                "status",
                status.name()
        );
    }

    private String issueStateKey(Long issueId) {
        return String.join(":", issueStateRedisKeyPrefix, "{" + issueId + "}", "state");
    }
}
