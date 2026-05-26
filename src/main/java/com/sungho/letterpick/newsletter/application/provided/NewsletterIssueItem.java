package com.sungho.letterpick.newsletter.application.provided;

import com.sungho.letterpick.newsletter.domain.NewsletterCategory;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record NewsletterIssueItem(
        Long issueId,
        Long newsletterId,
        String newsletterName,
        String newsletterImageUrl,
        NewsletterCategoryItem newsletterCategory,
        String subject,
        String previewText,
        Instant receivedAt,
        boolean read
) {

    public NewsletterIssueItem {
        requireNonNull(issueId);
        requireNonNull(newsletterId);
        requireNonNull(newsletterName);
        requireNonNull(newsletterImageUrl);
        requireNonNull(newsletterCategory);
        requireNonNull(subject);
        requireNonNull(previewText);
        requireNonNull(receivedAt);
    }

    public NewsletterIssueItem(
            Long issueId,
            Long newsletterId,
            String newsletterName,
            String newsletterImageUrl,
            NewsletterCategory newsletterCategory,
            String subject,
            String previewText,
            Instant receivedAt,
            boolean read
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
                read
        );
    }
}
