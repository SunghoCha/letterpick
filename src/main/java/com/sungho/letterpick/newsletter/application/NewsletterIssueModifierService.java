package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterIssueModifierService implements NewsletterIssueModifier {

    private final NewsletterIssueRepository newsletterIssueRepository;

    @Override
    public Instant delete(Long memberId, Long issueId) {
        NewsletterIssue newsletterIssue = newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(issueId, memberId)
                .orElseThrow(NewsletterIssueNotFoundException::new);
        newsletterIssue.deleteFromList();
        return newsletterIssue.getReceivedAt();
    }

}
