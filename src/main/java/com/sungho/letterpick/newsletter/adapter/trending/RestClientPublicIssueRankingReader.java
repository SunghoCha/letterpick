package com.sungho.letterpick.newsletter.adapter.trending;

import com.sungho.letterpick.newsletter.application.exception.PublicIssueRankingReadException;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;
import com.sungho.letterpick.newsletter.application.required.PublicIssueRankingReader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class RestClientPublicIssueRankingReader implements PublicIssueRankingReader {

    private final RestClient trendingServiceRestClient;

    @Override
    public List<PublicIssueRankingItem> findTop(PublicIssueRankingWindowType windowType, int limit) {
        requireNonNull(windowType, "windowType must not be null");

        try {
            PublicIssueRankingsResponse response = trendingServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/api/v1/public-issue-rankings")
                            .queryParam("windowType", windowType)
                            .queryParam("limit", limit)
                            .build()
                    )
                    .retrieve()
                    .body(PublicIssueRankingsResponse.class);

            if (response == null) {
                throw new PublicIssueRankingReadException(
                        new IllegalStateException("trending-service ranking response body is empty")
                );
            }

            return response.toRankingItems();
        } catch (RestClientException e) {
            throw new PublicIssueRankingReadException(e);
        }
    }

    private record PublicIssueRankingsResponse(
            List<PublicIssueRankingItemResponse> items
    ) {

        private PublicIssueRankingsResponse {
            items = List.copyOf(requireNonNull(items, "items must not be null"));
        }

        private List<PublicIssueRankingItem> toRankingItems() {
            return items.stream()
                    .map(PublicIssueRankingItemResponse::toRankingItem)
                    .toList();
        }
    }

    private record PublicIssueRankingItemResponse(
            Long issueId,
            long score
    ) {

        private PublicIssueRankingItem toRankingItem() {
            return new PublicIssueRankingItem(issueId, score);
        }
    }
}
