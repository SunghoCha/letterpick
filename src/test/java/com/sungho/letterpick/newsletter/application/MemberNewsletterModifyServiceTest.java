package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.domain.MemberNewsletter;
import com.sungho.letterpick.newsletter.domain.MemberNewsletterStatus;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.exception.MemberNewsletterNotFoundException;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterNotFoundException;
import jakarta.persistence.EntityManager;
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
@Import({LetterPickTestConfiguration.class, MemberNewsletterModifyService.class})
class MemberNewsletterModifyServiceTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberNewsletterModifyService memberNewsletterModifyService;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private MemberNewsletterRepository memberNewsletterRepository;

    @Test
    @DisplayName("구독 해지 상태에서 재구독하면 ACTIVE 상태로 변경된다")
    void resubscribe_changes_status_to_active_when_unsubscribed() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();

        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletter.unsubscribe();
        memberNewsletterRepository.save(memberNewsletter);
        em.flush();
        em.clear();
        // when
        memberNewsletterModifyService.resubscribe(memberId, newsletterId);
        em.flush();
        em.clear();
        // then
        MemberNewsletter foundMemberNewsletter = memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId)
                .orElseThrow();
        assertThat(foundMemberNewsletter.getStatus()).isEqualTo(MemberNewsletterStatus.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 뉴스레터에 대해 재구독하면 예외가 발생한다")
    void resubscribe_throws_when_newsletter_not_found() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        // then
        assertThatThrownBy(() -> memberNewsletterModifyService.resubscribe(memberId, newsletterId))
                .isInstanceOf(NewsletterNotFoundException.class);
    }

    @Test
    @DisplayName("구독 이력이 없는 뉴스레터에 대해 재구독하면 예외가 발생한다")
    void resubscribe_throws_when_member_newsletter_not_found() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();

        em.flush();
        em.clear();
        // then
        assertThatThrownBy(() -> memberNewsletterModifyService.resubscribe(memberId, newsletterId))
                .isInstanceOf(MemberNewsletterNotFoundException.class);
    }

    @Test
    @DisplayName("구독 중인 뉴스레터를 해지하면 구독해지 상태가 된다.")
    void unsubscribe_changes_status_to_unsubscribed_when_active() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();
        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletterRepository.save(memberNewsletter);
        em.flush();
        em.clear();
        // when
        memberNewsletterModifyService.unsubscribe(memberId, newsletterId);
        em.flush();
        em.clear();
        // then
        MemberNewsletter found = memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberNewsletterStatus.UNSUBSCRIBED);
    }

    @Test
    @DisplayName("존재하지 않는 뉴스레터에 대해 구독 해지하면 예외가 발생한다")
    void unsubscribe_throws_when_newsletter_not_found() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;

        // then
        assertThatThrownBy(() -> memberNewsletterModifyService.unsubscribe(memberId, newsletterId))
                .isInstanceOf(NewsletterNotFoundException.class);
    }

    @Test
    @DisplayName("구독 이력이 없는 뉴스레터에 대해 구독 해지하면 예외가 발생한다")
    void unsubscribe_throws_when_member_newsletter_not_found() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();

        em.flush();
        em.clear();

        // then
        assertThatThrownBy(() -> memberNewsletterModifyService.unsubscribe(memberId, newsletterId))
                .isInstanceOf(MemberNewsletterNotFoundException.class);
    }

    @Test
    @DisplayName("이미 구독 해지 상태인 뉴스레터를 다시 해지하면 상태를 유지한다")
    void unsubscribe_keeps_status_when_already_unsubscribed() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();
        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletter.unsubscribe();
        memberNewsletterRepository.save(memberNewsletter);
        em.flush();
        em.clear();

        // when
        memberNewsletterModifyService.unsubscribe(memberId, newsletterId);
        em.flush();
        em.clear();

        // then
        MemberNewsletter found = memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberNewsletterStatus.UNSUBSCRIBED);
    }

    @Test
    @DisplayName("이미 구독 중인 뉴스레터를 재구독하면 ACTIVE 상태를 유지한다")
    void resubscribe_keeps_status_when_already_active() {
        // given
        Long memberId = 1L;
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        Long newsletterId = savedNewsletter.getId();
        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletterRepository.save(memberNewsletter);
        em.flush();
        em.clear();
        // when
        memberNewsletterModifyService.resubscribe(memberId, newsletterId);
        em.flush();
        em.clear();
        // then
        MemberNewsletter found = memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MemberNewsletterStatus.ACTIVE);
    }
}
