package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.event.PublicIssueAvailableEvent;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.MemberNewsletter;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterMailReceiveService {

    private final InboundEmailRepository inboundEmailRepository;
    private final RecipientAddressResolver recipientAddressResolver;
    private final NewslettersRepository newslettersRepository;
    private final MemberNewsletterRepository memberNewsletterRepository;
    private final NewsletterIssueRepository newsletterIssueRepository;
    private final NewsletterIssuePreviewGenerator newsletterIssuePreviewGenerator;
    private final PublicFeedCollectorAccount publicFeedCollectorAccount;
    private final ApplicationEventPublisher eventPublisher;

    public void receive(ReceivedMail receivedMail) {
        if (inboundEmailRepository.existsByMessageKey(receivedMail.messageKey())) return;

        InboundEmail inboundEmail = saveInboundEmail(receivedMail);

        RecipientAddressResolution recipientAddressResolution = recipientAddressResolver.resolve(receivedMail.recipientAddress());
        switch (recipientAddressResolution.type()) {
            case INVALID_ADDRESS -> {
                inboundEmail.markInvalidRecipientAddress();
                return;
            }
            case NOT_FOUND -> {
                inboundEmail.markRecipientNotFound();
                return;
            }
            case FOUND -> {
            }
        }

        Long memberId = recipientAddressResolution.memberId();
        Optional<Newsletter> newsletterOpt = newslettersRepository.findByEmailAddress(receivedMail.senderEmail());
        if (newsletterOpt.isEmpty()) {
            inboundEmail.markNewsletterNotFound(memberId);
            return;
        }

        Newsletter newsletter = newsletterOpt.get();
        processSubscriptionReceive(memberId, newsletter, receivedMail.content(), inboundEmail);

    }

    private InboundEmail saveInboundEmail(ReceivedMail receivedMail) {
        InboundEmail inboundEmail = InboundEmail.create(
                receivedMail.messageKey(),
                receivedMail.rawReference(),
                receivedMail.recipientAddress(),
                receivedMail.senderEmail(),
                receivedMail.subject(),
                receivedMail.receivedAt()
        );
        return inboundEmailRepository.save(inboundEmail);
    }

    private void processSubscriptionReceive(
            Long memberId,
            Newsletter newsletter,
            String content,
            InboundEmail inboundEmail
    ) {
        Long newsletterId = newsletter.getId();
        Optional<MemberNewsletter> memberNewsletterOpt = memberNewsletterRepository
                .findByMemberIdAndNewsletterId(memberId, newsletterId);

        if (memberNewsletterOpt.isEmpty()) {
            memberNewsletterRepository.save(MemberNewsletter.create(memberId, newsletterId));
            completeIssueCreation(memberId, newsletter, content, inboundEmail);
            return;
        }

        MemberNewsletter memberNewsletter = memberNewsletterOpt.get();
        if (memberNewsletter.isUnsubscribed()) {
            inboundEmail.markSkippedUnsubscribed(memberId, newsletterId);
            return;
        }

        if (memberNewsletter.isActive()) {
            completeIssueCreation(memberId, newsletter, content, inboundEmail);
            return;
        }

        throw new IllegalStateException("지원하지 않는 구독 상태입니다.");
    }

    private void completeIssueCreation(Long memberId, Newsletter newsletter, String content, InboundEmail inboundEmail) {
        Long newsletterId = newsletter.getId();
        String previewText = newsletterIssuePreviewGenerator.generate(content);
        NewsletterIssue newsletterIssue = NewsletterIssue.create(
                memberId,
                newsletterId,
                inboundEmail.getId(),
                inboundEmail.getSubject(),
                content,
                previewText,
                inboundEmail.getReceivedAt()
        );
        NewsletterIssue savedIssue = newsletterIssueRepository.save(newsletterIssue);
        inboundEmail.markIssueCreated(memberId, newsletterId);

        if (publicFeedCollectorAccount.isCollectorInboxAddress(inboundEmail.getRecipientAddress())) {
            publishPublicIssueAvailableEvent(newsletter, inboundEmail, savedIssue);
        }
    }

    private void publishPublicIssueAvailableEvent(Newsletter newsletter, InboundEmail inboundEmail, NewsletterIssue savedIssue) {
        eventPublisher.publishEvent(new PublicIssueAvailableEvent(
                UUID.randomUUID().toString(),
                savedIssue.getId(),
                newsletter.getId(),
                newsletter.getCategory(),
                inboundEmail.getReceivedAt()
        ));
    }
}
