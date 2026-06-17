package com.sungho.letterpick.trending.ranking.adapter.persistence;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RdsSummaryPublicIssueRankingSummaryWriter implements PublicIssueRankingSummaryWriter {

    private final PublicIssueRankingSummaryRepository rankingSummaryRepository;

    @Override
    public void save(PublicIssueRankingWindow window, Long issueId, long score, Instant calculatedAt) {
        rankingSummaryRepository.upsertSummary(
                window.type().name(),
                window.key(),
                issueId,
                score,
                calculatedAt,
                calculatedAt
        );
    }

    @Override
    public void deleteByIssueId(Long issueId) {
        rankingSummaryRepository.deleteByIssueId(issueId);
    }
}
