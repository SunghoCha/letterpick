package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.common.auth.SocialProvider;
import com.sungho.letterpick.common.domain.Email;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.Nickname;
import com.sungho.letterpick.member.domain.NewsletterInboxAddress;
import com.sungho.letterpick.member.domain.SocialIdentity;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PublicNewsletterIssueModifierServiceTest {

    private static final String COLLECTOR_INBOX_ADDRESS = "eq3eaqv0hzfv@inbound.letterpicknews.com";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final NewsletterIssueModifier newsletterIssueModifier = mock(NewsletterIssueModifier.class);
    private final PublicNewsletterIssueModifierService service = new PublicNewsletterIssueModifierService(
            memberRepository,
            newsletterIssueModifier,
            COLLECTOR_INBOX_ADDRESS
    );

    @Test
    @DisplayName("공개 피드 컬렉터 회원의 이슈 삭제를 기존 뉴스레터 이슈 삭제 유스케이스에 위임한다")
    void delete_delegates_to_newsletter_issue_modifier_with_collector_member_id() {
        // given
        Member collector = collectorMember(42L);
        NewsletterInboxAddress collectorAddress = new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS);
        given(memberRepository.findByNewsletterInboxAddress(collectorAddress))
                .willReturn(Optional.of(collector));

        // when
        service.delete(10L);

        // then
        verify(memberRepository).findByNewsletterInboxAddress(collectorAddress);
        verify(newsletterIssueModifier).delete(42L, 10L);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원을 찾지 못하면 삭제를 수행하지 않는다")
    void delete_throws_when_collector_member_not_found() {
        // given
        NewsletterInboxAddress collectorAddress = new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS);
        given(memberRepository.findByNewsletterInboxAddress(collectorAddress))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(IllegalStateException.class);

        verify(memberRepository).findByNewsletterInboxAddress(collectorAddress);
        verify(newsletterIssueModifier, never()).delete(42L, 10L);
    }

    private Member collectorMember(Long memberId) {
        Member member = Member.register(
                new Email("collector@example.com"),
                new Nickname("collector"),
                new SocialIdentity(SocialProvider.GOOGLE, "collector-google-id"),
                new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS)
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
