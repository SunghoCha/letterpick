package com.sungho.letterpick.newsletter.application.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.MemberNewsletterRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.NewsletterIssuePreviewGenerator;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;
import com.sungho.letterpick.newsletter.domain.MemberNewsletterStatus;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import(LetterPickTestConfiguration.class)
class DevNewsletterIssueDemoServiceIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-05-12T03:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private MemberNewsletterRepository memberNewsletterRepository;

    @Autowired
    private InboundEmailRepository inboundEmailRepository;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    private DevNewsletterIssueDemoService demoService;

    @BeforeEach
    void setUp() {
        demoService = new DevNewsletterIssueDemoService(
                newslettersRepository,
                memberNewsletterRepository,
                inboundEmailRepository,
                newsletterIssueRepository,
                new NewsletterIssuePreviewGenerator(),
                CLOCK
        );
    }

    @Test
    @DisplayName("현재 회원 기준으로 데모 뉴스레터 이슈와 구독 관계를 생성한다")
    void createDemoIssues_creates_issues_and_active_subscriptions_for_member() {
        Member member = memberRepository.save(MemberFixture.createMember());
        saveDemoNewsletters();

        DevNewsletterIssueDemoResult result = demoService.createFor(member.getId());

        assertThat(result.createdIssueCount()).isEqualTo(8);
        assertThat(result.skippedIssueCount()).isZero();
        assertThat(newsletterIssueRepository.count()).isEqualTo(8);
        assertThat(memberNewsletterRepository.count()).isEqualTo(4);
        assertThat(inboundEmailRepository.count()).isEqualTo(8);

        List<InboundEmail> inboundEmails = inboundEmailRepository.findAll();
        assertThat(inboundEmails)
                .extracting(InboundEmail::getStatus)
                .containsOnly(InboundEmailStatus.ISSUE_CREATED);
        assertThat(inboundEmails)
                .extracting(InboundEmail::getMemberId)
                .containsOnly(member.getId());

        assertThat(memberNewsletterRepository.findAll())
                .extracting(memberNewsletter -> memberNewsletter.getStatus())
                .containsOnly(MemberNewsletterStatus.ACTIVE);

        List<NewsletterIssue> issues = newsletterIssueRepository.findAll();
        assertThat(issues)
                .extracting(NewsletterIssue::getMemberId)
                .containsOnly(member.getId());
        assertThat(issues)
                .anySatisfy(issue -> assertThat(issue.getReceivedAt())
                        .isAfter(Instant.parse("2050-05-11T15:00:00Z")))
                .anySatisfy(issue -> assertThat(issue.getReceivedAt())
                        .isBefore(Instant.parse("2050-05-11T15:00:00Z")));
    }

    @Test
    @DisplayName("이미 생성된 데모 이슈는 반복 실행해도 중복 생성하지 않는다")
    void createDemoIssues_skips_existing_demo_issues() {
        Member member = memberRepository.save(MemberFixture.createMember());
        saveDemoNewsletters();
        demoService.createFor(member.getId());

        DevNewsletterIssueDemoResult result = demoService.createFor(member.getId());

        assertThat(result.createdIssueCount()).isZero();
        assertThat(result.skippedIssueCount()).isEqualTo(8);
        assertThat(newsletterIssueRepository.count()).isEqualTo(8);
        assertThat(inboundEmailRepository.count()).isEqualTo(8);
    }

    private void saveDemoNewsletters() {
        newslettersRepository.save(NewsletterFixture.createNewsletter("테크 브리핑", NewsletterCategory.TECH));
        newslettersRepository.save(NewsletterFixture.createNewsletter("비즈 인사이트", NewsletterCategory.BIZ));
        newslettersRepository.save(NewsletterFixture.createNewsletter("트렌드 노트", NewsletterCategory.TREND));
        newslettersRepository.save(NewsletterFixture.createNewsletter("컬처 레터", NewsletterCategory.CULTURE));
    }
}
