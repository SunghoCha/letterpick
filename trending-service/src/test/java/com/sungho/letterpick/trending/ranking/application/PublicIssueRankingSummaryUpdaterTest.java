package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingSummaryWriter;
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
import java.time.LocalDate;
import java.time.ZoneId;
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
    private PublicIssueRankingSummaryWriter rankingSummaryWriter;

    private PublicIssueRankingSummaryUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new PublicIssueRankingSummaryUpdater(
                candidateRepository,
                viewCountSnapshotRepository,
                rankingSummaryWriter,
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
        ZoneId rankingZone = ZoneId.of("Asia/Seoul");
        LocalDate collectedDate = LocalDate.of(2050, 6, 12);
        verify(rankingSummaryWriter).save(
                PublicIssueRankingWindow.daily(collectedDate, rankingZone),
                10L,
                150L,
                CLOCK.instant()
        );
        verify(rankingSummaryWriter).save(
                PublicIssueRankingWindow.weekly(collectedDate, rankingZone),
                10L,
                150L,
                CLOCK.instant()
        );
        verify(rankingSummaryWriter).save(
                PublicIssueRankingWindow.monthly(collectedDate, rankingZone),
                10L,
                150L,
                CLOCK.instant()
        );
    }

    @Test
    @DisplayName("AVAILABLE 후보가 없으면 summary를 변경하지 않는다")
    void refresh_does_not_change_summary_when_candidate_not_found() {
        // given
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.empty());

        // when
        updater.refresh(10L);

        // then
        verifyNoInteractions(rankingSummaryWriter);
    }

    @Test
    @DisplayName("REMOVED 후보이면 조회수 snapshot을 보지 않고 summary를 제거한다")
    void refresh_removes_summary_when_candidate_removed() {
        // given
        PublicIssueCandidate candidate = mock(PublicIssueCandidate.class);
        given(candidate.getIssueId()).willReturn(10L);
        given(candidate.getStatus()).willReturn(PublicIssueCandidateStatus.REMOVED);
        given(candidate.getPublicFeedCollectedAt()).willReturn(Instant.parse("2050-06-12T00:30:00Z"));
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.of(candidate));

        // when
        updater.refresh(10L);

        // then
        verifyDeleteWindows();
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
        verifyDeleteWindows();
    }

    @Test
    @DisplayName("remove는 candidate 수집 시각 기준 window의 summary를 제거한다")
    void remove_summary_by_candidate_window() {
        // given
        PublicIssueCandidate candidate = PublicIssueCandidate.available(
                10L,
                20L,
                "TECH",
                Instant.parse("2050-06-12T00:30:00Z"),
                Instant.parse("2050-06-12T00:30:00Z")
        );
        given(candidateRepository.findByIssueIdForUpdate(10L)).willReturn(Optional.of(candidate));

        // when
        updater.remove(10L);

        // then
        verifyDeleteWindows();
    }

    private void verifyDeleteWindows() {
        ZoneId rankingZone = ZoneId.of("Asia/Seoul");
        LocalDate collectedDate = LocalDate.of(2050, 6, 12);
        verify(rankingSummaryWriter).delete(
                PublicIssueRankingWindow.daily(collectedDate, rankingZone),
                10L
        );
        verify(rankingSummaryWriter).delete(
                PublicIssueRankingWindow.weekly(collectedDate, rankingZone),
                10L
        );
        verify(rankingSummaryWriter).delete(
                PublicIssueRankingWindow.monthly(collectedDate, rankingZone),
                10L
        );
    }
}
