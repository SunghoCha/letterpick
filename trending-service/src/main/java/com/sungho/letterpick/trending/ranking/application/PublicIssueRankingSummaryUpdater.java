package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
import com.sungho.letterpick.trending.viewcount.PublicIssueViewCountSnapshot;
import com.sungho.letterpick.trending.viewcount.PublicIssueViewCountSnapshotRepository;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class PublicIssueRankingSummaryUpdater {

    private final PublicIssueCandidateRepository candidateRepository;
    private final PublicIssueViewCountSnapshotRepository viewCountSnapshotRepository;
    private final PublicIssueRankingSummaryWriter rankingSummaryWriter;
    private final PublicIssueRankingWindowCalculator windowCalculator;
    private final Clock clock;

    @WithSpan("trending.ranking_summary.refresh")
    public void refresh(@SpanAttribute("issue.id") Long issueId) {
        var candidate = candidateRepository.findByIssueIdForUpdate(issueId);
        if (candidate.isEmpty() || candidate.get().getStatus() != PublicIssueCandidateStatus.AVAILABLE) {
            rankingSummaryWriter.deleteByIssueId(issueId);
            return;
        }

        var snapshot = viewCountSnapshotRepository.findById(issueId);
        if (snapshot.isEmpty()) {
            rankingSummaryWriter.deleteByIssueId(issueId);
            return;
        }

        refresh(candidate.get(), snapshot.get());
    }

    public void remove(Long issueId) {
        rankingSummaryWriter.deleteByIssueId(issueId);
    }

    private void refresh(PublicIssueCandidate candidate, PublicIssueViewCountSnapshot snapshot) {
        Instant now = clock.instant();
        long score = snapshot.getViewCount();

        for (PublicIssueRankingWindow window : windowCalculator.windowsFor(candidate.getPublicFeedCollectedAt())) {
            rankingSummaryWriter.save(window, candidate.getIssueId(), score, now);
        }
    }
}
