package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
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
                .map(tuple -> toAvailableRankingItem(rankingKey, tuple))
                .flatMap(Optional::stream)
                .limit(resolvedLimit)
                .toList();
    }

    private Optional<PublicIssueRankingItem> toAvailableRankingItem(
            String rankingKey,
            ZSetOperations.TypedTuple<String> tuple
    ) {
        Long issueId = parseIssueId(rankingKey, tuple.getValue());

        if (issueId == null || !isAvailable(issueId)) {
            return Optional.empty();
        }

        Double score = Objects.requireNonNull(tuple.getScore(), "score must not be null");

        return Optional.of(new PublicIssueRankingItem(
                issueId,
                score.longValue()
        ));
    }

    private Long parseIssueId(String rankingKey, String rankedIssueId) {
        if (rankedIssueId == null) {
            log.warn("Skip malformed ranking member. rankingKey={}, rankedIssueId=null", rankingKey);
            return null;
        }

        try {
            return Long.parseLong(rankedIssueId);
        } catch (NumberFormatException e) {
            log.warn("Skip malformed ranking member. rankingKey={}, rankedIssueId={}", rankingKey, rankedIssueId);
            return null;
        }
    }

    private boolean isAvailable(Long issueId) {
        Object status = redisTemplate.opsForHash().get(
                RedisPublicIssueRankingKeys.issueStateKey(issueStateRedisKeyPrefix, issueId),
                RedisPublicIssueRankingKeys.STATUS_FIELD
        );
        return PublicIssueCandidateStatus.AVAILABLE.name().equals(status);
    }
}
