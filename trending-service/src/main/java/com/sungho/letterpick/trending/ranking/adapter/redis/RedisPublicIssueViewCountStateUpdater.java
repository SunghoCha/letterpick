package com.sungho.letterpick.trending.ranking.adapter.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RedisPublicIssueViewCountStateUpdater {

    private static final DefaultRedisScript<Long> ACCEPT_IF_AVAILABLE_AND_NOT_STALE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then
                return 0
            end

            local current = redis.call('HGET', KEYS[1], ARGV[3])
            if current == false then
                redis.call('HSET', KEYS[1], ARGV[3], ARGV[4])
                return 1
            end

            if tonumber(ARGV[4]) < tonumber(current) then
                return 0
            end

            if tonumber(ARGV[4]) > tonumber(current) then
                redis.call('HSET', KEYS[1], ARGV[3], ARGV[4])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String redisKeyPrefix;

    public RedisPublicIssueViewCountStateUpdater(
            StringRedisTemplate redisTemplate,
            @Value("${letterpick.trending.ranking.state.redis-key-prefix}") String redisKeyPrefix
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.redisKeyPrefix = RedisPublicIssueRankingKeys.validateRedisKeyPrefix(redisKeyPrefix);
    }

    public boolean acceptIfAvailableAndNotStale(Long issueId, long viewCount) {
        Objects.requireNonNull(issueId, "issueId must not be null");

        Long accepted = redisTemplate.execute(
                ACCEPT_IF_AVAILABLE_AND_NOT_STALE_SCRIPT,
                List.of(RedisPublicIssueRankingKeys.issueStateKey(redisKeyPrefix, issueId)),
                RedisPublicIssueRankingKeys.STATUS_FIELD,
                "AVAILABLE",
                RedisPublicIssueRankingKeys.VIEW_COUNT_FIELD,
                String.valueOf(viewCount)
        );
        if (accepted == null) {
            throw new IllegalStateException("failed to update issue view_count state");
        }
        return accepted == 1L;
    }
}
