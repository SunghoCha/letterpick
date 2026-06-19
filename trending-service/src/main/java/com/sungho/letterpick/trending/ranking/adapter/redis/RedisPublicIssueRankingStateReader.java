package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class RedisPublicIssueRankingStateReader {

    private final StringRedisTemplate redisTemplate;
    private final String redisKeyPrefix;

    public RedisPublicIssueRankingStateReader(
            StringRedisTemplate redisTemplate,
            @Value("${letterpick.trending.ranking.state.redis-key-prefix}") String redisKeyPrefix
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.redisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(redisKeyPrefix);
    }

    public Optional<AvailableIssueRankingState> findAvailableIssueState(Long issueId) {
        Objects.requireNonNull(issueId, "issueId must not be null");

        List<Object> state = redisTemplate.opsForHash().multiGet(
                RedisPublicIssueRankingKeys.issueStateKey(redisKeyPrefix, issueId),
                List.of(
                        RedisPublicIssueRankingKeys.STATUS_FIELD,
                        RedisPublicIssueRankingKeys.COLLECTED_AT_FIELD,
                        RedisPublicIssueRankingKeys.VIEW_COUNT_FIELD
                )
        );

        if (state == null || state.size() < 3) {
            return Optional.empty();
        }
        if (!PublicIssueCandidateStatus.AVAILABLE.name().equals(state.get(0))) {
            return Optional.empty();
        }

        Object collectedAt = state.get(1);
        if (collectedAt == null) {
            throw new IllegalStateException("available issue state must contain collected_at");
        }

        Object viewCount = state.get(2);
        if (viewCount == null) {
            return Optional.empty();
        }

        return Optional.of(new AvailableIssueRankingState(
                Instant.parse(collectedAt.toString()),
                Long.parseLong(viewCount.toString())
        ));
    }

    public record AvailableIssueRankingState(Instant publicFeedCollectedAt, long viewCount) {
    }
}
