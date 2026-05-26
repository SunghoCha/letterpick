package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.NewsletterInboxAddress;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublicNewsletterIssueQueryService implements PublicNewsletterIssueFinder {

    private final MemberRepository memberRepository;
    private final NewsletterIssueRepository newsletterIssueRepository;
    private final String collectorInboxAddress;

    public PublicNewsletterIssueQueryService(
            MemberRepository memberRepository,
            NewsletterIssueRepository newsletterIssueRepository,
            @Value("${newsletter.public-feed.collector-inbox-address:aaaaaaaaaaaa@inbound.letterpick.local}")
            String collectorInboxAddress
    ) {
        this.memberRepository = memberRepository;
        this.newsletterIssueRepository = newsletterIssueRepository;
        this.collectorInboxAddress = collectorInboxAddress;
    }

    @Override
    public Slice<NewsletterIssueItem> findIssues(PublicNewsletterIssueSearchCondition condition,
                                                 Pageable pageable) {
        Member member = memberRepository.findByNewsletterInboxAddress(new NewsletterInboxAddress(collectorInboxAddress))
                .orElseThrow(() -> new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."));
        return newsletterIssueRepository.findPublicIssuesByMemberId(member.getId(), condition, pageable);
    }

    @Override
    public NewsletterIssueDetail findIssueDetail(Long issueId) {
        Member member = memberRepository.findByNewsletterInboxAddress(new NewsletterInboxAddress(collectorInboxAddress))
                .orElseThrow(() -> new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."));

        return newsletterIssueRepository.findDetailByMemberIdAndIssueId(member.getId(), issueId)
                .orElseThrow(NewsletterIssueNotFoundException::new);
    }
}
