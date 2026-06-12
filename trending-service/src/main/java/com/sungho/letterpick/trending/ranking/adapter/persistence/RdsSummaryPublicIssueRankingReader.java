package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RdsSummaryPublicIssueRankingReader implements PublicIssueRankingReader {

    private final PublicIssueRankingSummaryRepository rankingSummaryRepository;

    @Override
    public List<PublicIssueRankingItem> findTop(PublicIssueRankingWindow window, int limit) {
        return rankingSummaryRepository.findTopByWindow(
                window.type().name(),
                window.key(),
                PublicIssueCandidateStatus.AVAILABLE,
                PageRequest.of(0, limit)
        );
    }
}
