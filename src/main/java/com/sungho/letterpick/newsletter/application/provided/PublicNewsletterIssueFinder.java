package com.sungho.letterpick.newsletter.application.provided;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface PublicNewsletterIssueFinder {

    Slice<NewsletterIssueItem> findIssues(PublicNewsletterIssueSearchCondition condition, Pageable pageable);

    NewsletterIssueDetail findIssueDetail(Long issueId);

    List<PublicNewsletterIssueRankingItem> findRankings(PublicIssueRankingWindowType windowType, Integer limit);
}
