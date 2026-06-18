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

@Component
@ConditionalOnProperty(
        prefix = "letterpick.trending.ranking.summary",
        name = "writer",
        havingValue = "redis"
)
public class RedisPublicIssueRankingSummaryWriter implements PublicIssueRankingSummaryWriter {

    private static final String ISSUES_KEY_PART = "issues";

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
        redisTemplate.opsForZSet().add(rankingKey, rankedIssueId, score);
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_delete")
    public void delete(PublicIssueRankingWindow window,
                       @SpanAttribute("issue.id") Long issueId) {
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(issueId, "issueId must not be null");

        redisTemplate.opsForZSet().remove(rankingKey(window), rankedIssueId(issueId));
    }

    private String rankingKey(PublicIssueRankingWindow window) {
        String windowHashTag = window.type().name() + ":" + window.key();
        return String.join(":", redisKeyPrefix, "{" + windowHashTag + "}", ISSUES_KEY_PART);
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
