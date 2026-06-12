package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.adapter.persistence.PublicIssueRankingSummaryRepository;
import com.sungho.letterpick.trending.viewcount.PublicIssueViewCountSnapshot;
import com.sungho.letterpick.trending.viewcount.PublicIssueViewCountSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicIssueRankingSummaryUpdaterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-12T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicIssueCandidateRepository candidateRepository;

    @Mock
    private PublicIssueViewCountSnapshotRepository viewCountSnapshotRepository;

    @Mock
    private PublicIssueRankingSummaryRepository rankingSummaryRepository;

    private PublicIssueRankingSummaryUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new PublicIssueRankingSummaryUpdater(
                candidateRepository,
                viewCountSnapshotRepository,
                rankingSummaryRepository,
                new PublicIssueRankingWindowCalculator(),
                CLOCK
        );
    }

    @Test
    @DisplayName("AVAILABLE 후보와 조회수 snapshot이 있으면 DAILY/WEEKLY/MONTHLY summary를 갱신한다")
    void refresh_available_issue_summary() {
        // given
        PublicIssueCandidate candidate = PublicIssueCandidate.available(
                10L,
                20L,
                "TECH",
                Instant.parse("2050-06-12T00:30:00Z"),
                Instant.parse("2050-06-12T00:30:00Z")
        );
        PublicIssueViewCountSnapshot snapshot = PublicIssueViewCountSnapshot.record(
                10L,
                150L,
                Instant.parse("2050-06-12T00:59:00Z"),
                Instant.parse("2050-06-12T01:00:00Z")
        );
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.of(candidate));
        given(viewCountSnapshotRepository.findById(10L)).willReturn(Optional.of(snapshot));

        // when
        updater.refresh(10L);

        // then
        verify(rankingSummaryRepository).upsertSummary(
                PublicIssueRankingWindowType.DAILY.name(),
                "2050-06-12",
                10L,
                150L,
                CLOCK.instant(),
                CLOCK.instant()
        );
        verify(rankingSummaryRepository).upsertSummary(
                PublicIssueRankingWindowType.WEEKLY.name(),
                "2050-06-06",
                10L,
                150L,
                CLOCK.instant(),
                CLOCK.instant()
        );
        verify(rankingSummaryRepository).upsertSummary(
                PublicIssueRankingWindowType.MONTHLY.name(),
                "2050-06-01",
                10L,
                150L,
                CLOCK.instant(),
                CLOCK.instant()
        );
    }

    @Test
    @DisplayName("AVAILABLE 후보가 없으면 summary를 제거한다")
    void refresh_removes_summary_when_candidate_not_found() {
        // given
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.empty());

        // when
        updater.refresh(10L);

        // then
        verify(rankingSummaryRepository).deleteByIssueId(10L);
    }

    @Test
    @DisplayName("REMOVED 후보이면 조회수 snapshot을 보지 않고 summary를 제거한다")
    void refresh_removes_summary_when_candidate_removed() {
        // given
        PublicIssueCandidate candidate = mock(PublicIssueCandidate.class);
        given(candidate.getStatus()).willReturn(PublicIssueCandidateStatus.REMOVED);
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.of(candidate));

        // when
        updater.refresh(10L);

        // then
        verify(rankingSummaryRepository).deleteByIssueId(10L);
        verifyNoInteractions(viewCountSnapshotRepository);
    }

    @Test
    @DisplayName("조회수 snapshot이 없으면 summary를 제거한다")
    void refresh_removes_summary_when_snapshot_not_found() {
        // given
        PublicIssueCandidate candidate = PublicIssueCandidate.available(
                10L,
                20L,
                "TECH",
                Instant.parse("2050-06-12T00:30:00Z"),
                Instant.parse("2050-06-12T00:30:00Z")
        );
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.of(candidate));
        given(viewCountSnapshotRepository.findById(10L)).willReturn(Optional.empty());

        // when
        updater.refresh(10L);

        // then
        verify(rankingSummaryRepository).deleteByIssueId(10L);
    }

    @Test
    @DisplayName("remove는 issueId 기준으로 summary를 제거한다")
    void remove_summary_by_issue_id() {
        // when
        updater.remove(10L);

        // then
        verify(rankingSummaryRepository).deleteByIssueId(10L);
    }
}
