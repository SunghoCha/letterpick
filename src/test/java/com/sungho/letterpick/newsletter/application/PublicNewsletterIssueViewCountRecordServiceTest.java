package com.sungho.letterpick.newsletter.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecordRequest;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountSnapshotRecorder;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountStore;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicNewsletterIssueViewCountRecordServiceTest {

    private static final int SNAPSHOT_INTERVAL = 5;

    @Mock
    private PublicFeedCollectorAccount publicFeedCollectorAccount;

    @Mock
    private NewsletterIssueRepository newsletterIssueRepository;

    @Mock
    private PublicIssueViewCountStore publicIssueViewCountStore;

    @Mock
    private PublicIssueViewCountSnapshotRecorder publicIssueViewCountSnapshotRecorder;

    @Mock
    private NewsletterIssue newsletterIssue;

    @Test
    @DisplayName("공개 피드 대상 이슈가 아니면 조회수를 반영하지 않는다")
    void record_noops_when_public_issue_not_found() {
        // given
        PublicNewsletterIssueViewCountRecordService service = service();
        PublicNewsletterIssueViewCountRecordRequest request = new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "anonymous:abc"
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(1L);
        given(newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.empty());

        // when
        service.record(request);

        // then
        verify(publicFeedCollectorAccount).collectorMemberId();
        verify(newsletterIssueRepository).findByIdAndMemberIdAndDeletedFalse(10L, 1L);
        verifyNoInteractions(publicIssueViewCountStore, publicIssueViewCountSnapshotRecorder);
    }

    @Test
    @DisplayName("중복 조회이면 조회수 snapshot 저장을 요청하지 않는다")
    void record_noops_snapshot_when_view_is_duplicate() {
        // given
        PublicNewsletterIssueViewCountRecordService service = service();
        PublicNewsletterIssueViewCountRecordRequest request = new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "member:20"
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(1L);
        given(newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(newsletterIssue));
        given(publicIssueViewCountStore.incrementIfFirstView(10L, "member:20"))
                .willReturn(0L);

        // when
        service.record(request);

        // then
        verify(publicIssueViewCountStore).incrementIfFirstView(10L, "member:20");
        verifyNoInteractions(publicIssueViewCountSnapshotRecorder);
    }

    @Test
    @DisplayName("snapshot 기준이 아니면 조회수 snapshot 저장을 요청하지 않는다")
    void record_noops_snapshot_when_view_count_does_not_reach_snapshot_interval() {
        // given
        PublicNewsletterIssueViewCountRecordService service = service();
        PublicNewsletterIssueViewCountRecordRequest request = new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "member:20"
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(1L);
        given(newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(newsletterIssue));
        given(publicIssueViewCountStore.incrementIfFirstView(10L, "member:20"))
                .willReturn(11L);

        // when
        service.record(request);

        // then
        verify(publicIssueViewCountStore).incrementIfFirstView(10L, "member:20");
        verifyNoInteractions(publicIssueViewCountSnapshotRecorder);
    }

    @Test
    @DisplayName("증가한 조회수가 snapshot 기준에 도달하면 snapshot 저장을 요청한다")
    void record_requests_snapshot_when_incremented_view_count_reaches_snapshot_interval() {
        // given
        PublicNewsletterIssueViewCountRecordService service = service();
        PublicNewsletterIssueViewCountRecordRequest request = new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "member:20"
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(1L);
        given(newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(newsletterIssue));
        given(publicIssueViewCountStore.incrementIfFirstView(10L, "member:20"))
                .willReturn(15L);

        // when
        service.record(request);

        // then
        verify(publicIssueViewCountStore).incrementIfFirstView(10L, "member:20");
        verify(publicIssueViewCountSnapshotRecorder).recordSnapshot(10L, 15L);
    }

    private PublicNewsletterIssueViewCountRecordService service() {
        return new PublicNewsletterIssueViewCountRecordService(
                publicFeedCollectorAccount,
                newsletterIssueRepository,
                publicIssueViewCountStore,
                publicIssueViewCountSnapshotRecorder,
                new PublicIssueViewCountProperties(
                        SNAPSHOT_INTERVAL,
                        Duration.ofMinutes(30),
                        "letterpick:public-issue",
                        "letterpick_anonymous_id",
                        Duration.ofDays(90)
                )
        );
    }
}
