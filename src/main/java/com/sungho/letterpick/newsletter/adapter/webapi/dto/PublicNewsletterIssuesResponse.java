package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import com.sungho.letterpick.newsletter.application.provided.NewsletterCategoryItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record PublicNewsletterIssuesResponse(
        List<PublicNewsletterIssueResponse> items,
        PageResponse page
) {

    public PublicNewsletterIssuesResponse {
        items = List.copyOf(requireNonNull(items));
        requireNonNull(page);
    }

    public static PublicNewsletterIssuesResponse from(Slice<NewsletterIssueItem> issues) {
        requireNonNull(issues);

        return new PublicNewsletterIssuesResponse(
                issues.getContent().stream()
                        .map(PublicNewsletterIssueResponse::from)
                        .toList(),
                PageResponse.from(issues)
        );
    }

    public record PublicNewsletterIssueResponse(
            Long issueId,
            Long newsletterId,
            String newsletterName,
            String newsletterImageUrl,
            CategoryResponse newsletterCategory,
            String subject,
            String previewText,
            Instant receivedAt
    ) {
        public PublicNewsletterIssueResponse {
            requireNonNull(issueId);
            requireNonNull(newsletterId);
            requireNonNull(newsletterName);
            requireNonNull(newsletterImageUrl);
            requireNonNull(newsletterCategory);
            requireNonNull(subject);
            requireNonNull(previewText);
            requireNonNull(receivedAt);
        }

        public static PublicNewsletterIssueResponse from(NewsletterIssueItem issue) {
            requireNonNull(issue);

            return new PublicNewsletterIssueResponse(
                    issue.issueId(),
                    issue.newsletterId(),
                    issue.newsletterName(),
                    issue.newsletterImageUrl(),
                    CategoryResponse.from(issue.newsletterCategory()),
                    issue.subject(),
                    issue.previewText(),
                    issue.receivedAt()
            );
        }
    }

    public record CategoryResponse(
            String code,
            String label
    ) {
        public CategoryResponse {
            requireNonNull(code);
            requireNonNull(label);
        }

        public static CategoryResponse from(NewsletterCategoryItem category) {
            requireNonNull(category);

            return new CategoryResponse(category.code(), category.label());
        }
    }
}
