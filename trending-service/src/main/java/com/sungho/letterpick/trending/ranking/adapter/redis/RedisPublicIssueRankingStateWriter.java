package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowCalculator;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class RedisPublicIssueRankingStateWriter {

    private static final Duration ISSUE_STATE_TTL = Duration.ofDays(14);
    private static final DefaultRedisScript<Long> MARK_AVAILABLE_IF_NOT_REMOVED_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], ARGV[1]) == ARGV[2] then
                return 0
            end

            redis.call('HSET', KEYS[1], ARGV[1], ARGV[3], ARGV[4], ARGV[5])
            redis.call('EXPIRE', KEYS[1], ARGV[6])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final PublicIssueRankingWindowCalculator windowCalculator;
    private final String issueStateRedisKeyPrefix;
    private final String rankingRedisKeyPrefix;

    public RedisPublicIssueRankingStateWriter(
            StringRedisTemplate redisTemplate,
            PublicIssueRankingWindowCalculator windowCalculator,
            @Value("${letterpick.trending.ranking.state.redis-key-prefix}") String issueStateRedisKeyPrefix,
            @Value("${letterpick.trending.ranking.summary.redis-key-prefix}") String rankingRedisKeyPrefix
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.windowCalculator = Objects.requireNonNull(windowCalculator, "windowCalculator must not be null");
        this.issueStateRedisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(issueStateRedisKeyPrefix);
        this.rankingRedisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(rankingRedisKeyPrefix);
    }

    @WithSpan("trending.ranking_state.redis_available")
    public void markAvailable(@SpanAttribute("issue.id") Long issueId, Instant publicFeedCollectedAt) {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");

        Long updated = redisTemplate.execute(
                MARK_AVAILABLE_IF_NOT_REMOVED_SCRIPT,
                List.of(issueStateKey(issueId)),
                RedisPublicIssueRankingKeys.STATUS_FIELD,
                PublicIssueCandidateStatus.REMOVED.name(),
                PublicIssueCandidateStatus.AVAILABLE.name(),
                RedisPublicIssueRankingKeys.COLLECTED_AT_FIELD,
                publicFeedCollectedAt.toString(),
                String.valueOf(ISSUE_STATE_TTL.toSeconds())
        );
        if (updated == null) {
            throw new IllegalStateException("failed to mark available issue state");
        }
    }

    @WithSpan("trending.ranking_state.redis_removed")
    public void markRemoved(@SpanAttribute("issue.id") Long issueId, Instant publicFeedCollectedAt) {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(publicFeedCollectedAt, "publicFeedCollectedAt must not be null");

        String issueStateKey = issueStateKey(issueId);
        redisTemplate.opsForHash().putAll(issueStateKey, issueStateFields(
                PublicIssueCandidateStatus.REMOVED,
                publicFeedCollectedAt
        ));
        redisTemplate.expire(issueStateKey, ISSUE_STATE_TTL);

        String rankedIssueId = RedisPublicIssueRankingKeys.rankedIssueId(issueId);
        for (PublicIssueRankingWindow window : windowCalculator.dailyAndWeeklyWindowsFor(publicFeedCollectedAt)) {
            redisTemplate.opsForZSet().remove(
                    RedisPublicIssueRankingKeys.rankingKey(rankingRedisKeyPrefix, window),
                    rankedIssueId
            );
        }
    }

    private Map<String, String> issueStateFields(PublicIssueCandidateStatus status, Instant publicFeedCollectedAt) {
        return Map.of(
                RedisPublicIssueRankingKeys.STATUS_FIELD, status.name(),
                RedisPublicIssueRankingKeys.COLLECTED_AT_FIELD, publicFeedCollectedAt.toString()
        );
    }

    private String issueStateKey(Long issueId) {
        return RedisPublicIssueRankingKeys.issueStateKey(issueStateRedisKeyPrefix, issueId);
    }
}
