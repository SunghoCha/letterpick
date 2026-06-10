package com.sungho.letterpick.trending.ranking.adapter.webapi.dto;

import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record PublicIssueRankingsResponse(
        List<PublicIssueRankingItemResponse> items
) {

    public PublicIssueRankingsResponse {
        items = List.copyOf(requireNonNull(items));
    }

    public static PublicIssueRankingsResponse from(List<PublicIssueRankingItem> items) {
        requireNonNull(items);

        return new PublicIssueRankingsResponse(items.stream()
                .map(PublicIssueRankingItemResponse::from)
                .toList());
    }

    public record PublicIssueRankingItemResponse(
            Long issueId,
            long score,
            long viewCount
    ) {

        public PublicIssueRankingItemResponse {
            requireNonNull(issueId);
        }

        public static PublicIssueRankingItemResponse from(PublicIssueRankingItem item) {
            requireNonNull(item);

            return new PublicIssueRankingItemResponse(
                    item.issueId(),
                    item.score(),
                    item.viewCount()
            );
        }
    }
}
