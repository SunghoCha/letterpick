package com.sungho.letterpick.newsletter.application.dev;

import static java.util.Objects.requireNonNull;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.NewsletterIssuePreviewGenerator;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.MemberNewsletter;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@Transactional
@RequiredArgsConstructor
public class DevNewsletterIssueDemoService {

    private static final int DEMO_NEWSLETTER_COUNT = 4;
    private static final List<DemoIssueTemplate> ISSUE_TEMPLATES = List.of(
            new DemoIssueTemplate("오늘 아침 핵심 브리핑", Duration.ofHours(1)),
            new DemoIssueTemplate("점심 전에 읽는 트렌드 노트", Duration.ofHours(3)),
            new DemoIssueTemplate("퇴근길에 보는 시장 요약", Duration.ofHours(6)),
            new DemoIssueTemplate("오늘 놓치기 쉬운 기술 이야기", Duration.ofHours(10)),
            new DemoIssueTemplate("어제의 주요 업데이트", Duration.ofDays(1).plusHours(2)),
            new DemoIssueTemplate("이번 주 흐름 정리", Duration.ofDays(2).plusHours(1)),
            new DemoIssueTemplate("실무자가 고른 읽을거리", Duration.ofDays(3).plusHours(4)),
            new DemoIssueTemplate("주말에 다시 볼 인사이트", Duration.ofDays(5).plusHours(2))
    );

    private final NewslettersRepository newslettersRepository;
    private final MemberNewsletterRepository memberNewsletterRepository;
    private final InboundEmailRepository inboundEmailRepository;
    private final NewsletterIssueRepository newsletterIssueRepository;
    private final NewsletterIssuePreviewGenerator newsletterIssuePreviewGenerator;
    private final Clock clock;

    public DevNewsletterIssueDemoResult createFor(Long memberId) {
        requireNonNull(memberId);

        List<Newsletter> newsletters = findDemoNewsletters();
        if (newsletters.isEmpty()) {
            return new DevNewsletterIssueDemoResult(0, 0);
        }

        int created = 0;
        int skipped = 0;

        for (int i = 0; i < ISSUE_TEMPLATES.size(); i++) {
            Newsletter newsletter = newsletters.get(i % newsletters.size());
            ensureActiveSubscription(memberId, newsletter.getId());

            String messageKey = messageKey(memberId, i);
            if (inboundEmailRepository.existsByMessageKey(messageKey)) {
                skipped++;
                continue;
            }

            createDemoIssue(memberId, newsletter, ISSUE_TEMPLATES.get(i), messageKey, i);
            created++;
        }

        return new DevNewsletterIssueDemoResult(created, skipped);
    }

    private List<Newsletter> findDemoNewsletters() {
        return newslettersRepository.findAll(
                PageRequest.of(0, DEMO_NEWSLETTER_COUNT, Sort.by("id").ascending())
        ).getContent();
    }

    private void ensureActiveSubscription(Long memberId, Long newsletterId) {
        memberNewsletterRepository.findByMemberIdAndNewsletterId(memberId, newsletterId)
                .ifPresentOrElse(
                        memberNewsletter -> {
                            if (memberNewsletter.isUnsubscribed()) {
                                memberNewsletter.resubscribe();
                            }
                        },
                        () -> memberNewsletterRepository.save(MemberNewsletter.create(memberId, newsletterId))
                );
    }

    private void createDemoIssue(
            Long memberId,
            Newsletter newsletter,
            DemoIssueTemplate template,
            String messageKey,
            int index
    ) {
        Instant receivedAt = clock.instant().minus(template.receivedBeforeNow());
        String subject = "[Demo] " + newsletter.getName() + " - " + template.subject();
        String content = content(newsletter, template);
        String previewText = newsletterIssuePreviewGenerator.generate(content);

        InboundEmail inboundEmail = inboundEmailRepository.save(InboundEmail.create(
                messageKey,
                "demo://newsletter-issues/member-" + memberId + "/" + (index + 1),
                "demo-member-" + memberId + "@inbound-dev.letterpicknews.com",
                newsletter.getEmail().address(),
                subject,
                receivedAt
        ));

        NewsletterIssue newsletterIssue = NewsletterIssue.create(
                memberId,
                newsletter.getId(),
                inboundEmail.getId(),
                subject,
                content,
                previewText,
                receivedAt
        );
        newsletterIssueRepository.save(newsletterIssue);
        inboundEmail.markIssueCreated(memberId, newsletter.getId());
    }

    private String messageKey(Long memberId, int index) {
        return "dev-demo-member-" + memberId + "-issue-" + (index + 1);
    }

    private String content(Newsletter newsletter, DemoIssueTemplate template) {
        return """
                <article>
                    <h1>%s</h1>
                    <p>%s에서 발행한 개발 환경 확인용 뉴스레터 이슈입니다.</p>
                    <p>투데이, 보관함, 상세 화면의 제목, 본문, 미리보기, 수신 시각 표시를 확인하기 위한 데이터입니다.</p>
                    <p>실제 외부 메일 수신 파이프라인을 거치지 않고 dev profile에서만 생성됩니다.</p>
                </article>
                """.formatted(template.subject(), newsletter.getName());
    }

    private record DemoIssueTemplate(
            String subject,
            Duration receivedBeforeNow
    ) {
    }
}
