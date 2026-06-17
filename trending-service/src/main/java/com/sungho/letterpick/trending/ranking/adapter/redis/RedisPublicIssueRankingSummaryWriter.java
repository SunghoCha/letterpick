package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(
        prefix = "letterpick.trending.ranking.summary",
        name = "writer",
        havingValue = "redis"
)
public class RedisPublicIssueRankingSummaryWriter implements PublicIssueRankingSummaryWriter {

    private static final String ISSUE_KEY_PART = "issue";
    private static final String ISSUE_RANKING_KEYS_PART = "ranking-keys";
    private static final RedisScript<Long> SAVE_SCRIPT = RedisScript.of("""
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
            redis.call('SADD', KEYS[2], KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String redisKeyPrefix;

    public RedisPublicIssueRankingSummaryWriter(
            StringRedisTemplate redisTemplate,
            @Value("${letterpick.trending.ranking.summary.redis-key-prefix}") String redisKeyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefix = validateRedisKeyPrefix(redisKeyPrefix);
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_save")
    public void save(PublicIssueRankingWindow window,
                     @SpanAttribute("issue.id") Long issueId,
                     long score,
                     Instant calculatedAt) {
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");

        String rankingKey = rankingKey(window);
        String rankedIssueId = rankedIssueId(issueId);
        redisTemplate.execute(
                SAVE_SCRIPT,
                List.of(rankingKey, issueRankingIndexKey(issueId)),
                rankedIssueId,
                String.valueOf(score)
        );
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_delete")
    public void deleteByIssueId(@SpanAttribute("issue.id") Long issueId) {
        Objects.requireNonNull(issueId, "issueId must not be null");

        String issueRankingIndexKey = issueRankingIndexKey(issueId);
        Set<String> rankingKeys = redisTemplate.opsForSet().members(issueRankingIndexKey);
        if (rankingKeys != null && !rankingKeys.isEmpty()) {
            String rankedIssueId = rankedIssueId(issueId);
            for (String rankingKey : rankingKeys) {
                redisTemplate.opsForZSet().remove(rankingKey, rankedIssueId);
            }
        }
        redisTemplate.delete(issueRankingIndexKey);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        return String.join(":", redisKeyPrefix, window.type().name(), window.key());
    }

    private String issueRankingIndexKey(Long issueId) {
        return String.join(":", redisKeyPrefix, ISSUE_KEY_PART, "{" + issueId + "}", ISSUE_RANKING_KEYS_PART);
    }

    private String rankedIssueId(Long issueId) {
        return String.valueOf(issueId);
    }

    private String validateRedisKeyPrefix(String redisKeyPrefix) {
        Objects.requireNonNull(redisKeyPrefix, "redisKeyPrefix must not be null");
        if (redisKeyPrefix.isBlank()) {
            throw new IllegalArgumentException("redisKeyPrefix must not be blank");
        }
        if (!redisKeyPrefix.equals(redisKeyPrefix.trim())) {
            throw new IllegalArgumentException("redisKeyPrefix must not contain leading or trailing whitespace");
        }
        return redisKeyPrefix;
    }
}
