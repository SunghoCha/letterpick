package com.sungho.letterpick.newsletter.application.provided;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "letterpick.public-issue.ranking")
public record PublicNewsletterIssueRankingLimitPolicy(
        int defaultLimit,
        int maxLimit
) {

    public PublicNewsletterIssueRankingLimitPolicy {
        if (defaultLimit < 1) {
            throw new IllegalArgumentException("defaultLimit must be positive");
        }
        if (maxLimit < defaultLimit) {
            throw new IllegalArgumentException("maxLimit must be greater than or equal to defaultLimit");
        }
    }

    public int resolve(Integer limit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, maxLimit);
    }
}
