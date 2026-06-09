package com.sungho.letterpick.newsletter.application;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "letterpick.public-issue.view-count")
public record PublicIssueViewCountProperties(
        int snapshotInterval,
        Duration dedupeTtl,
        String redisKeyPrefix,
        String anonymousCookieName,
        Duration anonymousCookieMaxAge,
        boolean anonymousCookieSecure
) {

    public PublicIssueViewCountProperties {
        Objects.requireNonNull(dedupeTtl, "dedupeTtl must not be null");
        Objects.requireNonNull(redisKeyPrefix, "redisKeyPrefix must not be null");
        Objects.requireNonNull(anonymousCookieName, "anonymousCookieName must not be null");
        Objects.requireNonNull(anonymousCookieMaxAge, "anonymousCookieMaxAge must not be null");

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
        if (anonymousCookieName.isBlank()) {
            throw new IllegalArgumentException("anonymousCookieName must not be blank");
        }
        if (anonymousCookieMaxAge.isZero() || anonymousCookieMaxAge.isNegative()) {
            throw new IllegalArgumentException("anonymousCookieMaxAge must be positive");
        }
        redisKeyPrefix = redisKeyPrefix.trim();
        anonymousCookieName = anonymousCookieName.trim();
    }
}
