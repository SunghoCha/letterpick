package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.member.domain.NewsletterInboxAddress;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicNewsletterIssueQueryServiceTest {

    private static final String COLLECTOR_INBOX_ADDRESS = "aaaaaaaaaaaa@inbound.letterpick.test";

    @Mock
    private MemberRepository memberRepository;

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

        // when & then
        assertThatThrownBy(() -> service.findIssues(condition, pageable))
                .isInstanceOf(IllegalStateException.class);
        verify(memberRepository).findByNewsletterInboxAddress(new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS));
        verifyNoInteractions(newsletterIssueRepository, publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 목록은 컬렉터 회원의 뉴스레터 이슈를 조회한다")
    void findIssuesReturnsCollectorMemberIssues() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Member collector = MemberFixture.createMemberWithId(10L);
        NewsletterInboxAddress collectorAddress = new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS);
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

        given(memberRepository.findByNewsletterInboxAddress(collectorAddress))
                .willReturn(Optional.of(collector));
        given(publicFeedSearchReader.findIssues(collector.getId(), condition, pageable))
                .willReturn(expected);

        // when
        Slice<NewsletterIssueItem> result = service.findIssues(condition, pageable);

        // then
        assertThat(result).isSameAs(expected);
        verify(memberRepository).findByNewsletterInboxAddress(collectorAddress);
        verify(publicFeedSearchReader).findIssues(collector.getId(), condition, pageable);
        verifyNoInteractions(newsletterIssueRepository);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원이 없으면 상세 조회를 실패로 드러낸다")
    void findIssueDetailThrowsWhenCollectorMemberNotFound() {
        // given
        PublicNewsletterIssueQueryService service = service();

        // when & then
        assertThatThrownBy(() -> service.findIssueDetail(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(memberRepository).findByNewsletterInboxAddress(new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS));
        verifyNoInteractions(newsletterIssueRepository, publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 상세는 컬렉터 회원의 뉴스레터 이슈 상세를 조회한다")
    void findIssueDetailReturnsCollectorMemberIssueDetail() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Member collector = MemberFixture.createMemberWithId(10L);
        NewsletterInboxAddress collectorAddress = new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS);
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

        given(memberRepository.findByNewsletterInboxAddress(collectorAddress))
                .willReturn(Optional.of(collector));
        given(newsletterIssueRepository.findDetailByMemberIdAndIssueId(collector.getId(), 1L))
                .willReturn(Optional.of(expected));

        // when
        NewsletterIssueDetail result = service.findIssueDetail(1L);

        // then
        assertThat(result).isSameAs(expected);
        verify(memberRepository).findByNewsletterInboxAddress(collectorAddress);
        verify(newsletterIssueRepository).findDetailByMemberIdAndIssueId(collector.getId(), 1L);
        verifyNoInteractions(publicFeedSearchReader);
    }

    @Test
    @DisplayName("공개 피드 상세 이슈를 찾지 못하면 이슈 없음 예외를 반환한다")
    void findIssueDetailThrowsWhenIssueNotFound() {
        // given
        PublicNewsletterIssueQueryService service = service();
        Member collector = MemberFixture.createMemberWithId(10L);
        NewsletterInboxAddress collectorAddress = new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS);

        given(memberRepository.findByNewsletterInboxAddress(collectorAddress))
                .willReturn(Optional.of(collector));
        given(newsletterIssueRepository.findDetailByMemberIdAndIssueId(collector.getId(), 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.findIssueDetail(999L))
                .isInstanceOf(NewsletterIssueNotFoundException.class);
        verify(memberRepository).findByNewsletterInboxAddress(collectorAddress);
        verify(newsletterIssueRepository).findDetailByMemberIdAndIssueId(collector.getId(), 999L);
        verifyNoInteractions(publicFeedSearchReader);
    }

    private PublicNewsletterIssueQueryService service() {
        return new PublicNewsletterIssueQueryService(
                memberRepository,
                newsletterIssueRepository,
                publicFeedSearchReader,
                COLLECTOR_INBOX_ADDRESS
        );
    }
}
