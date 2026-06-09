package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterIssueRepository extends JpaRepository<NewsletterIssue, Long>, CustomNewsletterIssueRepository {
    Optional<NewsletterIssue> findByInboundEmailId(Long inboundEmailId);
    Optional<NewsletterIssue> findByIdAndMemberIdAndDeletedFalse(Long issueId, Long memberId);
    boolean existsByIdAndMemberIdAndDeletedFalse(Long issueId, Long memberId);
}
