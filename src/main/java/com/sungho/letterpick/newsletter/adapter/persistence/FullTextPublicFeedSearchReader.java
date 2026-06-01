package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.required.PublicFeedSearchReader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(
        prefix = "letterpick.search.public-feed",
        name = "strategy",
        havingValue = "fulltext"
)
@Slf4j
public class FullTextPublicFeedSearchReader implements PublicFeedSearchReader {

    private final NewsletterIssueRepository newsletterIssueRepository;

    public FullTextPublicFeedSearchReader(NewsletterIssueRepository newsletterIssueRepository) {
        this.newsletterIssueRepository = newsletterIssueRepository;
    }

    @PostConstruct
    void logInitialized() {
        log.info("Public feed search reader initialized: fulltext");
    }

    @Override
    public Slice<NewsletterIssueItem> findIssues(Long memberId,
                                                 PublicNewsletterIssueSearchCondition condition,
                                                 Pageable pageable) {
        requireNonNull(memberId);
        requireNonNull(condition);
        requireNonNull(pageable);

        return newsletterIssueRepository.findPublicIssuesByMemberIdWithFullText(memberId, condition, pageable);
    }
}
