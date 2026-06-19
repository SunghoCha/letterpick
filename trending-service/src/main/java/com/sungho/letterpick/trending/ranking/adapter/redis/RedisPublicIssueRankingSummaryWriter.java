package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "letterpick.trending.ranking.summary",
        name = "writer",
        havingValue = "redis"
)
public class RedisPublicIssueRankingSummaryWriter implements PublicIssueRankingSummaryWriter {

    private static final long RANKING_SIZE_LIMIT = 100;
    private static final Duration DAILY_RANKING_RETENTION_AFTER_WINDOW = Duration.ofDays(2);
    private static final Duration WEEKLY_RANKING_RETENTION_AFTER_WINDOW = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final String redisKeyPrefix;

    public RedisPublicIssueRankingSummaryWriter(
            StringRedisTemplate redisTemplate,
            @Value("${letterpick.trending.ranking.summary.redis-key-prefix}") String redisKeyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(redisKeyPrefix);
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

        String rankingKey = RedisPublicIssueRankingKeys.rankingKey(redisKeyPrefix, window);
        String rankedIssueId = RedisPublicIssueRankingKeys.rankedIssueId(issueId);
        Instant expireAt = expireAt(window);

        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            StringRedisConnection stringConnection = new DefaultStringRedisConnection(connection);
            stringConnection.zAdd(rankingKey, score, rankedIssueId);
            stringConnection.zRemRange(rankingKey, 0, -RANKING_SIZE_LIMIT - 1);
            stringConnection.expireAt(rankingKey, expireAt.getEpochSecond());
            return null;
        });
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_delete")
    public void delete(PublicIssueRankingWindow window,
                       @SpanAttribute("issue.id") Long issueId) {
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(issueId, "issueId must not be null");

        redisTemplate.opsForZSet().remove(
                RedisPublicIssueRankingKeys.rankingKey(redisKeyPrefix, window),
                RedisPublicIssueRankingKeys.rankedIssueId(issueId)
        );
    }

    private Instant expireAt(PublicIssueRankingWindow window) {
        return switch (window.type()) {
            case DAILY -> window.endExclusive()
                    .plus(DAILY_RANKING_RETENTION_AFTER_WINDOW);
            case WEEKLY -> window.endExclusive()
                    .plus(WEEKLY_RANKING_RETENTION_AFTER_WINDOW);
        };
    }
}
