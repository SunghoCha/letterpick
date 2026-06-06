package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.event.PublicIssueAvailableEvent;
import com.sungho.letterpick.newsletter.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsletterMailReceiveServiceTest {

    @InjectMocks
    private NewsletterMailReceiveService newsletterMailReceiveService;

    @Mock
    private InboundEmailRepository inboundEmailRepository;

    @Mock
    private RecipientAddressResolver recipientAddressResolver;

    @Mock
    private NewslettersRepository newslettersRepository;

    @Mock
    private MemberNewsletterRepository memberNewsletterRepository;

    @Mock
    private NewsletterIssueRepository newsletterIssueRepository;

    @Mock
    private NewsletterIssuePreviewGenerator newsletterIssuePreviewGenerator;

    @Mock
    private PublicFeedCollectorAccount publicFeedCollectorAccount;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("새로운 receivedMail이 들어오면 수신 이력이 저장된다")
    void receive_saves_inbound_email() {
        // given
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0, InboundEmail.class));
        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.notFound());
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        InboundEmail savedInboundEmail = captor.getValue();
        assertThat(savedInboundEmail.getMessageKey()).isEqualTo(receivedMail.messageKey());
        assertThat(savedInboundEmail.getRecipientAddress()).isEqualTo(receivedMail.recipientAddress());
        assertThat(savedInboundEmail.getSubject()).isEqualTo(receivedMail.subject());
        assertThat(savedInboundEmail.getReceivedAt()).isEqualTo(receivedMail.receivedAt());
        assertThat(savedInboundEmail.getRawReference()).isEqualTo(receivedMail.rawReference());
        assertThat(savedInboundEmail.getSenderEmail()).isEqualTo(receivedMail.senderEmail());
    }

    @Test
    @DisplayName("수신자 주소로 회원을 찾지 못하면 수신 상태를 RECIPIENT_NOT_FOUND로 표시한다")
    void receive_marks_recipient_not_found_when_recipient_cannot_be_resolved() {
        // given
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0, InboundEmail.class));
        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.notFound());
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getRecipientAddress()).isEqualTo(receivedMail.recipientAddress());
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.RECIPIENT_NOT_FOUND);
        assertThat(inboundEmail.getMemberId()).isNull();
    }

    @Test
    @DisplayName("수신자 주소 형식이 바르지 않으면 수신 상태를 INVALID_RECIPIENT_ADDRESS로 표시한다")
    void receive_marks_invalid_recipient_address_when_recipient_address_is_invalid() {
        // given
        ReceivedMail receivedMail = ReceivedMailFixture.create(
                "test@inbound-dev.letterpicknews.com",
                "newsletter@example.com"
        );
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0, InboundEmail.class));
        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.invalidAddress());
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());
        verifyNoInteractions(
                newslettersRepository,
                memberNewsletterRepository,
                newsletterIssueRepository
        );

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getRecipientAddress()).isEqualTo(receivedMail.recipientAddress());
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.INVALID_RECIPIENT_ADDRESS);
        assertThat(inboundEmail.getMemberId()).isNull();
        assertThat(inboundEmail.getNewsletterId()).isNull();
    }

    @Test
    @DisplayName("발신자 이메일로 뉴스레터를 찾지 못하면 수신 상태를 NEWSLETTER_NOT_FOUND로 표시한다")
    void receive_marks_newsletter_not_found_when_newsletter_cannot_be_resolved() {
        // given
        Long memberId = 1L;
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0, InboundEmail.class));
        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.found(memberId));
        given(newslettersRepository.findByEmailAddress(receivedMail.senderEmail()))
                .willReturn(Optional.empty());
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());
        verify(newslettersRepository).findByEmailAddress(receivedMail.senderEmail());

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getRecipientAddress()).isEqualTo(receivedMail.recipientAddress());
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.NEWSLETTER_NOT_FOUND);
        assertThat(inboundEmail.getMemberId()).isEqualTo(memberId);
        assertThat(inboundEmail.getNewsletterId()).isNull();
    }

    @Test
    @DisplayName("앱 내 구독이 해지된 상태이면 수신 상태를 SKIPPED_UNSUBSCRIBED로 표시한다")
    void receive_marks_skipped_unsubscribed_when_subscription_is_unsubscribed() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> invocation.getArgument(0, InboundEmail.class));
        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.found(memberId));

        Newsletter newsletter = NewsletterFixture.createNewsletterWithId(newsletterId);
        given(newslettersRepository.findByEmailAddress(receivedMail.senderEmail()))
                .willReturn(Optional.of(newsletter));

        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        memberNewsletter.unsubscribe();
        given(memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId))
                .willReturn(Optional.of(memberNewsletter));
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());
        verify(newslettersRepository).findByEmailAddress(receivedMail.senderEmail());
        verify(memberNewsletterRepository).findByMemberIdAndNewsletterId(memberId, newsletterId);

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.SKIPPED_UNSUBSCRIBED);
        assertThat(inboundEmail.getMemberId()).isEqualTo(memberId);
        assertThat(inboundEmail.getNewsletterId()).isEqualTo(newsletterId);
    }

    @Test
    @DisplayName("회원과 뉴스레터가 식별되고 구독 관계가 활성 상태이면 뉴스레터 이슈를 생성하고 수신 상태를 ISSUE_CREATED로 표시한다")
    void receive_creates_newsletter_issue_and_marks_issue_created_when_subscription_is_active() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        Long inboundEmailId = 100L;
        String previewText = "뉴스레터 본문 미리보기";
        ReceivedMail receivedMail = ReceivedMailFixture.create();

        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> {
                    InboundEmail inboundEmail = invocation.getArgument(0, InboundEmail.class);
                    ReflectionTestUtils.setField(inboundEmail, "id", inboundEmailId);
                    return inboundEmail;
                });

        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.found(memberId));

        Newsletter newsletter = NewsletterFixture.createNewsletterWithId(newsletterId);
        given(newslettersRepository.findByEmailAddress(receivedMail.senderEmail()))
                .willReturn(Optional.of(newsletter));

        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        given(memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId))
                .willReturn(Optional.of(memberNewsletter));
        given(newsletterIssuePreviewGenerator.generate(receivedMail.content()))
                .willReturn(previewText);
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());
        verify(newslettersRepository).findByEmailAddress(receivedMail.senderEmail());
        verify(memberNewsletterRepository).findByMemberIdAndNewsletterId(memberId, newsletterId);
        verify(newsletterIssuePreviewGenerator).generate(receivedMail.content());

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.ISSUE_CREATED);
        assertThat(inboundEmail.getMemberId()).isEqualTo(memberId);
        assertThat(inboundEmail.getNewsletterId()).isEqualTo(newsletterId);

        ArgumentCaptor<NewsletterIssue> issueCaptor = ArgumentCaptor.forClass(NewsletterIssue.class);
        verify(newsletterIssueRepository).save(issueCaptor.capture());
        NewsletterIssue newsletterIssue = issueCaptor.getValue();
        assertThat(newsletterIssue.getMemberId()).isEqualTo(memberId);
        assertThat(newsletterIssue.getNewsletterId()).isEqualTo(newsletterId);
        assertThat(newsletterIssue.getInboundEmailId()).isEqualTo(inboundEmailId);
        assertThat(newsletterIssue.getSubject()).isEqualTo(receivedMail.subject());
        assertThat(newsletterIssue.getContent()).isEqualTo(receivedMail.content());
        assertThat(newsletterIssue.getPreviewText()).isEqualTo(previewText);
        assertThat(newsletterIssue.getReceivedAt()).isEqualTo(receivedMail.receivedAt());
        assertThat(newsletterIssue.isRead()).isFalse();
        assertThat(newsletterIssue.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("공개 피드 컬렉터 회원의 뉴스레터 이슈가 생성되면 공개 이슈 생성 이벤트를 발행한다")
    void receive_publishes_public_issue_available_event_when_collector_issue_is_created() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        Long inboundEmailId = 100L;
        Long issueId = 200L;
        String previewText = "뉴스레터 본문 미리보기";
        ReceivedMail receivedMail = ReceivedMailFixture.create();

        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> {
                    InboundEmail inboundEmail = invocation.getArgument(0, InboundEmail.class);
                    ReflectionTestUtils.setField(inboundEmail, "id", inboundEmailId);
                    return inboundEmail;
                });

        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.found(memberId));

        Newsletter newsletter = NewsletterFixture.createNewsletterWithId(newsletterId);
        given(newslettersRepository.findByEmailAddress(receivedMail.senderEmail()))
                .willReturn(Optional.of(newsletter));

        MemberNewsletter memberNewsletter = MemberNewsletter.create(memberId, newsletterId);
        given(memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId))
                .willReturn(Optional.of(memberNewsletter));
        given(newsletterIssuePreviewGenerator.generate(receivedMail.content()))
                .willReturn(previewText);
        given(newsletterIssueRepository.save(any(NewsletterIssue.class)))
                .willAnswer(invocation -> {
                    NewsletterIssue issue = invocation.getArgument(0, NewsletterIssue.class);
                    ReflectionTestUtils.setField(issue, "id", issueId);
                    return issue;
                });
        given(publicFeedCollectorAccount.isCollectorInboxAddress(receivedMail.recipientAddress()))
                .willReturn(true);

        // when
        newsletterMailReceiveService.receive(receivedMail);

        // then
        ArgumentCaptor<PublicIssueAvailableEvent> eventCaptor = ArgumentCaptor.forClass(PublicIssueAvailableEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PublicIssueAvailableEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.issueId()).isEqualTo(issueId);
        assertThat(event.newsletterId()).isEqualTo(newsletterId);
        assertThat(event.category()).isEqualTo(newsletter.getCategory());
        assertThat(event.publicFeedCollectedAt()).isEqualTo(receivedMail.receivedAt());
    }

    @Test
    @DisplayName("회원과 뉴스레터가 식별되고 구독 관계가 없으면 구독 관계와 뉴스레터 이슈를 생성하고 수신 상태를 ISSUE_CREATED로 표시한다")
    void receive_creates_subscription_and_newsletter_issue_when_subscription_does_not_exist() {
        // given
        Long memberId = 1L;
        Long newsletterId = 2L;
        Long inboundEmailId = 100L;
        String previewText = "뉴스레터 본문 미리보기";
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.save(any(InboundEmail.class)))
                .willAnswer(invocation -> {
                    InboundEmail inboundEmail = invocation.getArgument(0, InboundEmail.class);
                    ReflectionTestUtils.setField(inboundEmail, "id", inboundEmailId);
                    return inboundEmail;
                });

        given(recipientAddressResolver.resolve(receivedMail.recipientAddress()))
                .willReturn(RecipientAddressResolution.found(memberId));

        Newsletter newsletter = NewsletterFixture.createNewsletterWithId(newsletterId);
        given(newslettersRepository.findByEmailAddress(receivedMail.senderEmail()))
                .willReturn(Optional.of(newsletter));

        given(memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId))
                .willReturn(Optional.empty());

        given(newsletterIssuePreviewGenerator.generate(receivedMail.content()))
                .willReturn(previewText);
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        ArgumentCaptor<InboundEmail> captor = ArgumentCaptor.forClass(InboundEmail.class);
        verify(inboundEmailRepository).save(captor.capture());
        verify(recipientAddressResolver).resolve(receivedMail.recipientAddress());
        verify(newslettersRepository).findByEmailAddress(receivedMail.senderEmail());
        verify(memberNewsletterRepository).findByMemberIdAndNewsletterId(memberId, newsletterId);
        verify(newsletterIssuePreviewGenerator).generate(receivedMail.content());

        InboundEmail inboundEmail = captor.getValue();
        assertThat(inboundEmail.getStatus()).isEqualTo(InboundEmailStatus.ISSUE_CREATED);
        assertThat(inboundEmail.getMemberId()).isEqualTo(memberId);
        assertThat(inboundEmail.getNewsletterId()).isEqualTo(newsletterId);

        ArgumentCaptor<MemberNewsletter> memberNewsletterCaptor = ArgumentCaptor.forClass(MemberNewsletter.class);
        verify(memberNewsletterRepository).save(memberNewsletterCaptor.capture());
        MemberNewsletter memberNewsletter = memberNewsletterCaptor.getValue();

        assertThat(memberNewsletter.getMemberId()).isEqualTo(memberId);
        assertThat(memberNewsletter.getNewsletterId()).isEqualTo(newsletterId);
        assertThat(memberNewsletter.getStatus()).isEqualTo(MemberNewsletterStatus.ACTIVE);

        ArgumentCaptor<NewsletterIssue> issueCaptor = ArgumentCaptor.forClass(NewsletterIssue.class);
        verify(newsletterIssueRepository).save(issueCaptor.capture());
        NewsletterIssue newsletterIssue = issueCaptor.getValue();

        assertThat(newsletterIssue.getMemberId()).isEqualTo(memberId);
        assertThat(newsletterIssue.getNewsletterId()).isEqualTo(newsletterId);
        assertThat(newsletterIssue.getInboundEmailId()).isEqualTo(inboundEmailId);
        assertThat(newsletterIssue.getSubject()).isEqualTo(receivedMail.subject());
        assertThat(newsletterIssue.getContent()).isEqualTo(receivedMail.content());
        assertThat(newsletterIssue.getPreviewText()).isEqualTo(previewText);
        assertThat(newsletterIssue.getReceivedAt()).isEqualTo(receivedMail.receivedAt());
        assertThat(newsletterIssue.isRead()).isFalse();
        assertThat(newsletterIssue.isDeleted()).isFalse();

    }

    @Test
    @DisplayName("이미 존재하는 messageKey이면 수신 처리를 추가로 진행하지 않는다")
    void receive_skips_processing_when_message_key_already_exists() {
        // given
        ReceivedMail receivedMail = ReceivedMailFixture.create();
        given(inboundEmailRepository.existsByMessageKey(receivedMail.messageKey()))
                .willReturn(true);
        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        verify(inboundEmailRepository).existsByMessageKey(receivedMail.messageKey());
        verifyNoMoreInteractions(inboundEmailRepository);
        verifyNoInteractions(
                recipientAddressResolver,
                newslettersRepository,
                memberNewsletterRepository,
                newsletterIssueRepository
        );

    }
}
