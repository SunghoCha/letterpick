package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RedisPublicIssueRankingReader implements PublicIssueRankingReader {

    private final StringRedisTemplate redisTemplate;
    private final String rankingRedisKeyPrefix;
    private final String issueStateRedisKeyPrefix;
    private final PublicIssueRankingLimitPolicy limitPolicy;

    public RedisPublicIssueRankingReader(
            StringRedisTemplate redisTemplate,
            String rankingRedisKeyPrefix,
            String issueStateRedisKeyPrefix,
            PublicIssueRankingLimitPolicy limitPolicy
    ) {
        this.redisTemplate = redisTemplate;
        this.rankingRedisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(rankingRedisKeyPrefix);
        this.issueStateRedisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(issueStateRedisKeyPrefix);
        this.limitPolicy = Objects.requireNonNull(limitPolicy, "limitPolicy must not be null");
    }

    @Override
    @WithSpan("trending.ranking_summary.redis_read")
    public List<PublicIssueRankingItem> findTop(PublicIssueRankingWindow window, int limit) {
        Objects.requireNonNull(window, "window must not be null");

        int resolvedLimit = limitPolicy.resolve(limit);
        String rankingKey = RedisPublicIssueRankingKeys.rankingKey(rankingRedisKeyPrefix, window);
        Set<ZSetOperations.TypedTuple<String>> rankings = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankingKey, 0, limitPolicy.maxSize() - 1L);

        if (rankings == null || rankings.isEmpty()) {
            return List.of();
        }

        return rankings.stream()
                .filter(this::isAvailable)
                .map(this::toRankingItem)
                .limit(resolvedLimit)
                .toList();
    }

    private boolean isAvailable(ZSetOperations.TypedTuple<String> tuple) {
        String rankedIssueId = Objects.requireNonNull(tuple.getValue(), "rankedIssueId must not be null");
        Long issueId = Long.parseLong(rankedIssueId);
        Object status = redisTemplate.opsForHash().get(
                RedisPublicIssueRankingKeys.issueStateKey(issueStateRedisKeyPrefix, issueId),
                RedisPublicIssueRankingKeys.STATUS_FIELD
        );
        return PublicIssueCandidateStatus.AVAILABLE.name().equals(status);
    }

    private PublicIssueRankingItem toRankingItem(ZSetOperations.TypedTuple<String> tuple) {
        String rankedIssueId = Objects.requireNonNull(tuple.getValue(), "rankedIssueId must not be null");
        Double score = Objects.requireNonNull(tuple.getScore(), "score must not be null");

        return new PublicIssueRankingItem(
                Long.parseLong(rankedIssueId),
                score.longValue()
        );
    }
}
