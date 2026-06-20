package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueRankingLimitPolicy;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.required.PublicIssueRankingReader;
import com.sungho.letterpick.newsletter.application.required.PublicFeedSearchReader;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicNewsletterIssueQueryService implements PublicNewsletterIssueFinder {

    private final PublicFeedCollectorAccount publicFeedCollectorAccount;

    private final NewsletterIssueRepository newsletterIssueRepository;

    private final PublicFeedSearchReader publicFeedSearchReader;

    private final PublicIssueRankingReader publicIssueRankingReader;

    private final PublicNewsletterIssueRankingLimitPolicy rankingLimitPolicy;

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

    @Override
    public List<PublicNewsletterIssueRankingItem> findRankings(PublicIssueRankingWindowType windowType, Integer limit) {
        requireNonNull(windowType, "windowType must not be null");

        int resolvedLimit = rankingLimitPolicy.resolve(limit);
        Long memberId = publicFeedCollectorAccount.collectorMemberId();
        List<PublicIssueRankingItem> rankings = publicIssueRankingReader.findTop(windowType, resolvedLimit);

        if (rankings.isEmpty()) {
            return List.of();
        }

        List<Long> issueIds = rankings.stream()
                .map(PublicIssueRankingItem::issueId)
                .toList();
        Map<Long, NewsletterIssueItem> issueItemById = newsletterIssueRepository
                .findPublicIssuesByMemberIdAndIssueIds(memberId, issueIds)
                .stream()
                .collect(Collectors.toMap(
                        NewsletterIssueItem::issueId,
                        Function.identity()
                ));

        return rankings.stream()
                .filter(ranking -> issueItemById.containsKey(ranking.issueId()))
                .map(ranking -> new PublicNewsletterIssueRankingItem(
                        issueItemById.get(ranking.issueId()),
                        ranking.score()
                ))
                .toList();
    }
}
