package com.sungho.letterpick.newsletter.application.required;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PublicFeedSearchReader {

    Slice<NewsletterIssueItem> findIssues(
            Long memberId,
            PublicNewsletterIssueSearchCondition condition,
            Pageable pageable
    );
}
