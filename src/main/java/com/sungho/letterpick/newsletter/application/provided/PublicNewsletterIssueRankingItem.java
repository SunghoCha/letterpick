package com.sungho.letterpick.newsletter.application.provided;

import com.sungho.letterpick.newsletter.domain.NewsletterCategory;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record PublicNewsletterIssueRankingItem(
        Long issueId,
        Long newsletterId,
        String newsletterName,
        String newsletterImageUrl,
        NewsletterCategoryItem newsletterCategory,
        String subject,
        String previewText,
        Instant receivedAt,
        long score
) {

    public PublicNewsletterIssueRankingItem {
        requireNonNull(issueId);
        requireNonNull(newsletterId);
        requireNonNull(newsletterName);
        requireNonNull(newsletterImageUrl);
        requireNonNull(newsletterCategory);
        requireNonNull(subject);
        requireNonNull(previewText);
        requireNonNull(receivedAt);
        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
    }

    public PublicNewsletterIssueRankingItem(
            NewsletterIssueItem issue,
            long score
    ) {
        this(
                issue.issueId(),
                issue.newsletterId(),
                issue.newsletterName(),
                issue.newsletterImageUrl(),
                issue.newsletterCategory(),
                issue.subject(),
                issue.previewText(),
                issue.receivedAt(),
                score
        );
    }

    public PublicNewsletterIssueRankingItem(
            Long issueId,
            Long newsletterId,
            String newsletterName,
            String newsletterImageUrl,
            NewsletterCategory newsletterCategory,
            String subject,
            String previewText,
            Instant receivedAt,
            long score
    ) {
        this(
                issueId,
                newsletterId,
                newsletterName,
                newsletterImageUrl,
                NewsletterCategoryItem.from(newsletterCategory),
                subject,
                previewText,
                receivedAt,
                score
        );
    }
}
