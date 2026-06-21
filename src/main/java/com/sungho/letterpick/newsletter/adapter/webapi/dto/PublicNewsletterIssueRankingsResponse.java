package com.sungho.letterpick.newsletter.adapter.webapi.dto;

import com.sungho.letterpick.newsletter.application.provided.NewsletterCategoryItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueRankingItem;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record PublicNewsletterIssueRankingsResponse(
        List<PublicNewsletterIssueRankingResponse> items
) {

    public PublicNewsletterIssueRankingsResponse {
        items = List.copyOf(requireNonNull(items));
    }

    public static PublicNewsletterIssueRankingsResponse from(List<PublicNewsletterIssueRankingItem> rankings) {
        requireNonNull(rankings);

        return new PublicNewsletterIssueRankingsResponse(rankings.stream()
                .map(PublicNewsletterIssueRankingResponse::from)
                .toList());
    }

    public record PublicNewsletterIssueRankingResponse(
            Long issueId,
            Long newsletterId,
            String newsletterName,
            String newsletterImageUrl,
            CategoryResponse newsletterCategory,
            String subject,
            String previewText,
            Instant receivedAt,
            long score
    ) {

        public PublicNewsletterIssueRankingResponse {
            requireNonNull(issueId);
            requireNonNull(newsletterId);
            requireNonNull(newsletterName);
            requireNonNull(newsletterImageUrl);
            requireNonNull(newsletterCategory);
            requireNonNull(subject);
            requireNonNull(previewText);
            requireNonNull(receivedAt);
        }

        public static PublicNewsletterIssueRankingResponse from(PublicNewsletterIssueRankingItem item) {
            requireNonNull(item);

            return new PublicNewsletterIssueRankingResponse(
                    item.issueId(),
                    item.newsletterId(),
                    item.newsletterName(),
                    item.newsletterImageUrl(),
                    CategoryResponse.from(item.newsletterCategory()),
                    item.subject(),
                    item.previewText(),
                    item.receivedAt(),
                    item.score()
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
