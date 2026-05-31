package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.required.PublicFeedSearchReader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@Component
@ConditionalOnProperty(
        prefix = "letterpick.search.public-feed",
        name = "strategy",
        havingValue = "fulltext"
)
@Slf4j
public class FullTextPublicFeedSearchReader implements PublicFeedSearchReader {

    private static final String QUERY_MODE_RAW = "raw";
    private static final String QUERY_MODE_ALL_TERMS = "all_terms";
    private static final String QUERY_MODE_PROPERTY =
            "${letterpick.search.public-feed.fulltext-query-mode:" + QUERY_MODE_RAW + "}";

    private final NewsletterIssueRepository newsletterIssueRepository;
    private final String fullTextQueryMode;

    public FullTextPublicFeedSearchReader(
            NewsletterIssueRepository newsletterIssueRepository,
            @Value(QUERY_MODE_PROPERTY) String fullTextQueryMode
    ) {
        this.newsletterIssueRepository = newsletterIssueRepository;
        this.fullTextQueryMode = normalizeFullTextQueryMode(fullTextQueryMode);
    }

    @PostConstruct
    void logInitialized() {
        log.info("Public feed search reader initialized: fulltext, queryMode={}", fullTextQueryMode);
    }

    @Override
    public Slice<NewsletterIssueItem> findIssues(Long memberId,
                                                 PublicNewsletterIssueSearchCondition condition,
                                                 Pageable pageable) {
        requireNonNull(memberId);
        requireNonNull(condition);
        requireNonNull(pageable);

        if (QUERY_MODE_ALL_TERMS.equals(fullTextQueryMode)) {
            return newsletterIssueRepository.findPublicIssuesByMemberIdWithFullTextAllTerms(memberId, condition, pageable);
        }

        return newsletterIssueRepository.findPublicIssuesByMemberIdWithFullTextRaw(memberId, condition, pageable);
    }

    private String normalizeFullTextQueryMode(String queryMode) {
        String normalizedQueryMode = queryMode == null ? QUERY_MODE_RAW : queryMode.trim().toLowerCase(Locale.ROOT);
        if (QUERY_MODE_RAW.equals(normalizedQueryMode) || QUERY_MODE_ALL_TERMS.equals(normalizedQueryMode)) {
            return normalizedQueryMode;
        }

        throw new IllegalArgumentException("Unsupported fulltext query mode: " + queryMode);
    }
}
