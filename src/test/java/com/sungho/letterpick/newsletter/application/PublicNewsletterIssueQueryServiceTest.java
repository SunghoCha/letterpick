package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.required.PublicFeedSearchReader;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicNewsletterIssueQueryServiceTest {

    @Mock
    private PublicFeedCollectorAccount publicFeedCollectorAccount;

    @Mock
    private NewsletterIssueRepository newsletterIssueRepository;

    @Mock
    private PublicFeedSearchReader publicFeedSearchReader;

    @Test
    @DisplayName("공개 피드 컬렉터 회원이 없으면 목록 조회를 실패로 드러낸다")
    void findIssuesThrowsWhenCollectorMemberNotFound() {
        // given
        PublicNewsletterIssueQueryService service = service();
        PublicNewsletterIssueSearchCondition condition = new PublicNewsletterIssueSearchCondition(null, null);
        Pageable pageable = PageRequest.of(0, 20);

        willThrow(new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."))
                .given(publicFeedCollectorAccount).collectorMemberId();

        // when & then
        assertThatThrownBy(() -> service.findIssues(condition, pageable))
                .isInstanceOf(IllegalStateException.class);
        verify(publicFeedCollectorAccount).collectorMemberId();
        verifyNoInteractions(newsletterIssueRepository, publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 목록은 컬렉터 회원의 뉴스레터 이슈를 조회한다")
    void findIssuesReturnsCollectorMemberIssues() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Long collectorMemberId = 10L;
        PublicNewsletterIssueSearchCondition condition = new PublicNewsletterIssueSearchCondition(NewsletterCategory.TECH, "redis");
        Pageable pageable = PageRequest.of(0, 20);
        Slice<NewsletterIssueItem> expected = new SliceImpl<>(
                List.of(new NewsletterIssueItem(
                        1L,
                        2L,
                        "기술 뉴스레터",
                        "https://example.com/newsletter.png",
                        NewsletterCategory.TECH,
                        "공개 피드 테스트",
                        "공개 피드 미리보기",
                        Instant.parse("2050-05-12T03:00:00Z"),
                        false
                )),
                pageable,
                false
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(collectorMemberId);
        given(publicFeedSearchReader.findIssues(collectorMemberId, condition, pageable))
                .willReturn(expected);

        // when
        Slice<NewsletterIssueItem> result = service.findIssues(condition, pageable);

        // then
        assertThat(result).isSameAs(expected);
        verify(publicFeedCollectorAccount).collectorMemberId();
        verify(publicFeedSearchReader).findIssues(collectorMemberId, condition, pageable);
        verifyNoInteractions(newsletterIssueRepository);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원이 없으면 상세 조회를 실패로 드러낸다")
    void findIssueDetailThrowsWhenCollectorMemberNotFound() {
        // given
        PublicNewsletterIssueQueryService service = service();

        willThrow(new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."))
                .given(publicFeedCollectorAccount).collectorMemberId();

        // when & then
        assertThatThrownBy(() -> service.findIssueDetail(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(publicFeedCollectorAccount).collectorMemberId();
        verifyNoInteractions(newsletterIssueRepository, publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 상세는 컬렉터 회원의 뉴스레터 이슈 상세를 조회한다")
    void findIssueDetailReturnsCollectorMemberIssueDetail() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Long collectorMemberId = 10L;
        NewsletterIssueDetail expected = new NewsletterIssueDetail(
                1L,
                2L,
                "기술 뉴스레터",
                "https://example.com/newsletter.png",
                "공개 피드 테스트",
                "<p>공개 피드 본문</p>",
                Instant.parse("2050-05-12T03:00:00Z"),
                true
        );

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(collectorMemberId);
        given(newsletterIssueRepository.findDetailByMemberIdAndIssueId(collectorMemberId, 1L))
                .willReturn(Optional.of(expected));

        // when
        NewsletterIssueDetail result = service.findIssueDetail(1L);

        // then
        assertThat(result).isSameAs(expected);
        verify(publicFeedCollectorAccount).collectorMemberId();
        verify(newsletterIssueRepository).findDetailByMemberIdAndIssueId(collectorMemberId, 1L);
        verifyNoInteractions(publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 상세 이슈를 찾지 못하면 이슈 없음 예외를 반환한다")
    void findIssueDetailThrowsWhenIssueNotFound() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Long collectorMemberId = 10L;

        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(collectorMemberId);
        given(newsletterIssueRepository.findDetailByMemberIdAndIssueId(collectorMemberId, 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.findIssueDetail(999L))
                .isInstanceOf(NewsletterIssueNotFoundException.class);
        verify(publicFeedCollectorAccount).collectorMemberId();
        verify(newsletterIssueRepository).findDetailByMemberIdAndIssueId(collectorMemberId, 999L);
        verifyNoInteractions(publicFeedSearchReader);
    }

    private PublicNewsletterIssueQueryService service() {
        return new PublicNewsletterIssueQueryService(
                publicFeedCollectorAccount,
                newsletterIssueRepository,
                publicFeedSearchReader

        );
    }
}
