package com.sungho.letterpick.trending.ranking.application.provided;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicIssueRankingLimitPolicy {

    private final int defaultLimit;
    private final int maxSize;

    public PublicIssueRankingLimitPolicy(
            @Value("${letterpick.trending.ranking.summary.default-limit:20}") int defaultLimit,
            @Value("${letterpick.trending.ranking.summary.max-size:100}") int maxSize
    ) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        if (defaultLimit <= 0 || defaultLimit > maxSize) {
            throw new IllegalArgumentException(
                    "defaultLimit must be between 1 and " + maxSize
            );
        }
        this.defaultLimit = defaultLimit;
        this.maxSize = maxSize;
    }

    public int defaultLimit() {
        return defaultLimit;
    }

    public int maxSize() {
        return maxSize;
    }

    public int resolve(Integer requestedLimit) {
        if (requestedLimit == null) {
            return defaultLimit;
        }
        if (requestedLimit < 1) {
            return 1;
        }
        return Math.min(requestedLimit, maxSize);
    }
}
