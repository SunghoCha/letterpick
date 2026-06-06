package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicNewsletterIssueModifierServiceTest {

    @Mock
    private PublicFeedCollectorAccount publicFeedCollectorAccount;

    @Mock
    private NewsletterIssueModifier newsletterIssueModifier;

    @InjectMocks
    private PublicNewsletterIssueModifierService service;

    @Test
    @DisplayName("공개 피드 컬렉터 회원의 이슈 삭제를 기존 뉴스레터 이슈 삭제 유스케이스에 위임한다")
    void delete_delegates_to_newsletter_issue_modifier_with_collector_member_id() {
        // given
        given(publicFeedCollectorAccount.collectorMemberId()).willReturn(42L);

        // when
        service.delete(10L);

        // then
        verify(publicFeedCollectorAccount).collectorMemberId();
        verify(newsletterIssueModifier).delete(42L, 10L);
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원을 찾지 못하면 삭제를 수행하지 않는다")
    void delete_throws_when_collector_member_not_found() {
        // given
        given(publicFeedCollectorAccount.collectorMemberId())
                .willThrow(new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(IllegalStateException.class);

        verify(publicFeedCollectorAccount).collectorMemberId();
        verifyNoInteractions(newsletterIssueModifier);
    }
}
