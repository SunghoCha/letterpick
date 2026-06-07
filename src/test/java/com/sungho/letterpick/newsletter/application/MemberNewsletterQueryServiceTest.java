package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.domain.MemberNewsletter;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class, MemberNewsletterQueryService.class})
class MemberNewsletterQueryServiceTest {

    @Autowired
    private MemberNewsletterQueryService memberNewsletterQueryService;

    @Autowired
    private MemberNewsletterRepository memberNewsletterRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Test
    @DisplayName("미구독 상태이면 NONE과 외부 구독 URL을 반환한다")
    void findSubscriptionInfoReturnsNoneWhenNotSubscribed() {
        // given
        Newsletter newsletter = NewsletterFixture.createNewsletter();
        Newsletter savedNewsletter = newslettersRepository.save(newsletter);
        Long memberId = 1L;
        Long newsletterId = savedNewsletter.getId();
        // when
        SubscriptionInfo info = memberNewsletterQueryService.findSubscriptionInfo(memberId, newsletterId);
        // then
        assertThat(info.status()).isEqualTo(SubscriptionStatus.NONE);
        assertThat(info.externalSubscribeUrl()).isEqualTo(newsletter.getSubscribeUrl());
    }

    @Test
    @DisplayName("이미 구독 중이면 ACTIVE와 URL 없음을 반환한다")
    void findSubscriptionInfoReturnsActiveWhenAlreadySubscribed() {
        // given
        Newsletter newsletter = NewsletterFixture.createNewsletter();
        Newsletter savedNewsletter = newslettersRepository.save(newsletter);
        Long memberId = 1L;
        Long newsletterId = savedNewsletter.getId();
        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletterRepository.save(memberNewsletter);
        // when
        SubscriptionInfo info = memberNewsletterQueryService.findSubscriptionInfo(memberId, newsletterId);
        // then
        assertThat(info.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(info.externalSubscribeUrl()).isNull();
    }

    @Test
    @DisplayName("구독해지 상태이면 UNSUBSCRIBED와 URL 없음을 반환한다")
    void findSubscriptionInfoReturnsUnsubscribedWhenUnsubscribed() {
        // given
        Newsletter newsletter = NewsletterFixture.createNewsletter();
        Newsletter savedNewsletter = newslettersRepository.save(newsletter);
        Long memberId = 1L;
        Long newsletterId = savedNewsletter.getId();
        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletter.unsubscribe();
        memberNewsletterRepository.save(memberNewsletter);
        // when
        SubscriptionInfo info = memberNewsletterQueryService.findSubscriptionInfo(memberId, newsletterId);
        // then
        assertThat(info.status()).isEqualTo(SubscriptionStatus.UNSUBSCRIBED);
        assertThat(info.externalSubscribeUrl()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 뉴스레터이면 예외를 던진다")
    void findSubscriptionInfoThrowsWhenNewsletterNotFound() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        // then
        assertThatThrownBy(() -> memberNewsletterQueryService.findSubscriptionInfo(memberId, newsletterId))
                .isInstanceOf(NewsletterNotFoundException.class);
    }
}
