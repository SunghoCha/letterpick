package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PublicNewsletterIssueModifierService implements PublicNewsletterIssueModifier {

    private final PublicFeedCollectorAccount publicFeedCollectorAccount;
    private final NewsletterIssueModifier newsletterIssueModifier;

    @Override
    public void delete(Long issueId) {
        newsletterIssueModifier.delete(publicFeedCollectorAccount.collectorMemberId(), issueId);
    }
}
