package com.sungho.letterpick.trending.ranking.adapter.redis;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;

import java.util.Objects;

final class RedisPublicIssueRankingKeys {

    private static final String ISSUES_KEY_PART = "issues";
    private static final String STATE_KEY_PART = "state";
    static final String STATUS_FIELD = "status";
    static final String COLLECTED_AT_FIELD = "collected_at";
    static final String VIEW_COUNT_FIELD = "view_count";

    private RedisPublicIssueRankingKeys() {
    }

    static String rankingKey(String redisKeyPrefix, PublicIssueRankingWindow window) {
        String windowHashTag = window.type().name() + ":" + window.key();
        return String.join(":", redisKeyPrefix, "{" + windowHashTag + "}", ISSUES_KEY_PART);
    }

    static String issueStateKey(String redisKeyPrefix, Long issueId) {
        return String.join(":", redisKeyPrefix, "{" + issueId + "}", STATE_KEY_PART);
    }

    static String rankedIssueId(Long issueId) {
        return String.valueOf(issueId);
    }

    static String validateRedisKeyPrefix(String redisKeyPrefix) {
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
