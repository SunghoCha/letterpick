package com.sungho.letterpick.newsletter.application;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "letterpick.public-issue.view-count")
public record PublicIssueViewCountProperties(
        int snapshotInterval,
        Duration dedupeTtl,
        String redisKeyPrefix
) {

    public PublicIssueViewCountProperties {
        Objects.requireNonNull(dedupeTtl, "dedupeTtl must not be null");
        Objects.requireNonNull(redisKeyPrefix, "redisKeyPrefix must not be null");

        if (snapshotInterval <= 0) {
            throw new IllegalArgumentException("snapshotInterval must be positive");
        }
        if (dedupeTtl.isZero() || dedupeTtl.isNegative()) {
            throw new IllegalArgumentException("dedupeTtl must be positive");
        }
        if (dedupeTtl.toMillis() <= 0) {
            throw new IllegalArgumentException("dedupeTtl must be at least 1 millisecond");
        }
        if (redisKeyPrefix.isBlank()) {
            throw new IllegalArgumentException("redisKeyPrefix must not be blank");
        }
        redisKeyPrefix = redisKeyPrefix.trim();
    }
}
