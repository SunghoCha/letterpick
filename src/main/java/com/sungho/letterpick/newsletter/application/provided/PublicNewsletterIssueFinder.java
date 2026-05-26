package com.sungho.letterpick.newsletter.application.provided;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PublicNewsletterIssueFinder {

    Slice<NewsletterIssueItem> findIssues(PublicNewsletterIssueSearchCondition condition, Pageable pageable);

    NewsletterIssueDetail findIssueDetail(Long issueId);
}
