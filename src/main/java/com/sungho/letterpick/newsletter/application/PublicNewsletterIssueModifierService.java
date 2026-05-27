package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.NewsletterInboxAddress;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublicNewsletterIssueModifierService implements PublicNewsletterIssueModifier {

    private final MemberRepository memberRepository;
    private final NewsletterIssueModifier newsletterIssueModifier;
    private final String collectorInboxAddress;

    public PublicNewsletterIssueModifierService(
            MemberRepository memberRepository,
            NewsletterIssueModifier newsletterIssueModifier,
            @Value("${newsletter.public-feed.collector-inbox-address}") String collectorInboxAddress
    ) {
        this.memberRepository = memberRepository;
        this.newsletterIssueModifier = newsletterIssueModifier;
        this.collectorInboxAddress = collectorInboxAddress;
    }

    @Override
    public void delete(Long issueId) {
        Member member = memberRepository.findByNewsletterInboxAddress(new NewsletterInboxAddress(collectorInboxAddress))
                .orElseThrow(() -> new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다."));

        newsletterIssueModifier.delete(member.getId(), issueId);
    }
}
