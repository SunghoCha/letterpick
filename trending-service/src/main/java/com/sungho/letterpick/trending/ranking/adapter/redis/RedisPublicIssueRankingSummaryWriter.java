package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
        String member = issueMember(issueId);
        redisTemplate.opsForZSet().add(rankingKey, member, score);
        redisTemplate.opsForSet().add(issueRankingKeysKey(issueId), rankingKey);
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_delete")
    public void deleteByIssueId(@SpanAttribute("issue.id") Long issueId) {
        Objects.requireNonNull(issueId, "issueId must not be null");

        String issueRankingKeysKey = issueRankingKeysKey(issueId);
        Set<String> rankingKeys = redisTemplate.opsForSet().members(issueRankingKeysKey);
        if (rankingKeys != null && !rankingKeys.isEmpty()) {
            String member = issueMember(issueId);
            for (String rankingKey : rankingKeys) {
                redisTemplate.opsForZSet().remove(rankingKey, member);
            }
        }
        redisTemplate.delete(issueRankingKeysKey);
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        return String.join(":", redisKeyPrefix, window.type().name(), window.key());
    }

    private String issueRankingKeysKey(Long issueId) {
        return String.join(":", redisKeyPrefix, ISSUE_KEY_PART, "{" + issueId + "}", ISSUE_RANKING_KEYS_PART);
    }

    private String issueMember(Long issueId) {
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
