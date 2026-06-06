package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.required.PublicFeedSearchReader;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicNewsletterIssueQueryService implements PublicNewsletterIssueFinder {

    private final PublicFeedCollectorAccount publicFeedCollectorAccount;

    private final NewsletterIssueRepository newsletterIssueRepository;

    private final PublicFeedSearchReader publicFeedSearchReader;

    @Override
    public Slice<NewsletterIssueItem> findIssues(PublicNewsletterIssueSearchCondition condition,
                                                 Pageable pageable) {
        Long memberId = publicFeedCollectorAccount.collectorMemberId();
        return publicFeedSearchReader.findIssues(memberId, condition, pageable);
    }

    @Override
    public NewsletterIssueDetail findIssueDetail(Long issueId) {
        Long memberId = publicFeedCollectorAccount.collectorMemberId();
        return newsletterIssueRepository.findDetailByMemberIdAndIssueId(memberId, issueId)
                .orElseThrow(NewsletterIssueNotFoundException::new);
    }
}
