package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record PublicNewsletterIssueDetailResponse(
        Long issueId,
        Long newsletterId,
        String newsletterName,
        String newsletterImageUrl,
        String subject,
        String content,
        Instant receivedAt
) {

    public PublicNewsletterIssueDetailResponse {
        requireNonNull(issueId);
        requireNonNull(newsletterId);
        requireNonNull(newsletterName);
        requireNonNull(newsletterImageUrl);
        requireNonNull(subject);
        requireNonNull(content);
        requireNonNull(receivedAt);
    }

    public static PublicNewsletterIssueDetailResponse from(NewsletterIssueDetail detail) {
        requireNonNull(detail);

        return new PublicNewsletterIssueDetailResponse(
                detail.issueId(),
                detail.newsletterId(),
                detail.newsletterName(),
                detail.newsletterImageUrl(),
                detail.subject(),
                detail.content(),
                detail.receivedAt()
        );
    }
}
