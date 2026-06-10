package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import com.sungho.letterpick.newsletter.application.event.PublicIssueRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PublicNewsletterIssueModifierService implements PublicNewsletterIssueModifier {

    private final PublicFeedCollectorAccount publicFeedCollectorAccount;
    private final NewsletterIssueModifier newsletterIssueModifier;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void delete(Long issueId) {
        newsletterIssueModifier.delete(publicFeedCollectorAccount.collectorMemberId(), issueId);
        eventPublisher.publishEvent(new PublicIssueRemovedEvent(
                UUID.randomUUID().toString(),
                issueId,
                clock.instant()
        ));
    }
}
