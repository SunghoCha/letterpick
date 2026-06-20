package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

public interface CustomNewsletterIssueRepository {
    Slice<NewsletterIssueItem> findAllByMemberId(Long memberId,
                                                 NewsletterIssueSearchCondition condition,
                                                 Pageable pageable);

    Slice<NewsletterIssueItem> findPublicIssuesByMemberId(Long memberId,
                                                          PublicNewsletterIssueSearchCondition condition,
                                                          Pageable pageable);

    Slice<NewsletterIssueItem> findPublicIssuesByMemberIdWithFullText(Long memberId,
                                                                      PublicNewsletterIssueSearchCondition condition,
                                                                      Pageable pageable);

    List<NewsletterIssueItem> findPublicIssuesByMemberIdAndIssueIds(Long memberId, List<Long> issueIds);

    Optional<NewsletterIssueDetail> findDetailByMemberIdAndIssueId(Long memberId, Long issueId);
}
