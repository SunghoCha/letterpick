package com.sungho.letterpick.trending.ranking.application.required;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;

import java.time.Instant;

public interface PublicIssueRankingSummaryWriter {

    void save(PublicIssueRankingWindow window, Long issueId, long score, Instant calculatedAt);

    void delete(PublicIssueRankingWindow window, Long issueId);
}
