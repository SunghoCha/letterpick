package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        LetterPickTestConfiguration.class,
        RecipientAddressResolver.class,
        PublicFeedCollectorAccount.class,
        NewsletterMailReceiveService.class,
        NewsletterIssuePreviewGenerator.class
})
class NewsletterMailReceiveServiceIntegrationTest {

    @Autowired
    private InboundEmailRepository inboundEmailRepository;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private NewsletterMailReceiveService newsletterMailReceiveService;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberNewsletterRepository memberNewsletterRepository;

    @Test
    @DisplayName("새로운 ReceivedMail이 들어오면 수신 이력, 뉴스레터 이슈, 구독 관계가 저장된다")
    void receive_persists_inbound_email_issue_and_subscription() {
        // given
        Member savedMember = memberRepository.save(MemberFixture.createMember());
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        ReceivedMail receivedMail = createReceivedMail(savedMember, savedNewsletter);
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        InboundEmail inboundEmail = inboundEmailRepository.findByMessageKey(receivedMail.messageKey()).orElseThrow();
        NewsletterIssue newsletterIssue = newsletterIssueRepository.findByInboundEmailId(inboundEmail.getId()).orElseThrow();
        MemberNewsletter memberNewsletter = memberNewsletterRepository
                .findByMemberIdAndNewsletterId(savedMember.getId(), savedNewsletter.getId())
                .orElseThrow();

        assertThat(inboundEmail.getMessageKey()).isEqualTo(receivedMail.messageKey());
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.ISSUE_CREATED);
        assertThat(inboundEmail.getMemberId()).isEqualTo(savedMember.getId());
        assertThat(inboundEmail.getNewsletterId()).isEqualTo(savedNewsletter.getId());

        assertThat(newsletterIssue.getMemberId()).isEqualTo(savedMember.getId());
        assertThat(newsletterIssue.getNewsletterId()).isEqualTo(savedNewsletter.getId());
        assertThat(newsletterIssue.getInboundEmailId()).isEqualTo(inboundEmail.getId());
        assertThat(newsletterIssue.getSubject()).isEqualTo(receivedMail.subject());
        assertThat(newsletterIssue.getContent()).isEqualTo(receivedMail.content());

        assertThat(memberNewsletter.getMemberId()).isEqualTo(savedMember.getId());
        assertThat(memberNewsletter.getNewsletterId()).isEqualTo(savedNewsletter.getId());
        assertThat(memberNewsletter.getStatus()).isEqualTo(MemberNewsletterStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 처리된 messageKey이면 추가 수신 데이터를 저장하지 않는다")
    void receive_does_not_persist_additional_data_when_message_key_already_exists() {
        // given
        Member savedMember = memberRepository.save(MemberFixture.createMember());
        Newsletter savedNewsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());
        ReceivedMail receivedMail = createReceivedMail(savedMember, savedNewsletter);
        newsletterMailReceiveService.receive(receivedMail);
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        assertThat(inboundEmailRepository.count()).isEqualTo(1);
        assertThat(newsletterIssueRepository.count()).isEqualTo(1);
        assertThat(memberNewsletterRepository.count()).isEqualTo(1);
    }

    private ReceivedMail createReceivedMail(Member member, Newsletter newsletter) {
        return ReceivedMailFixture.create(
                member.getNewsletterInboxAddress().address(),
                newsletter.getEmail().address()
        );
    }
}
