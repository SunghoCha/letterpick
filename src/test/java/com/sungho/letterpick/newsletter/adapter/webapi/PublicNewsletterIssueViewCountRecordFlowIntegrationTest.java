package com.sungho.letterpick.newsletter.adapter.webapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.LetterPickRedisTestConfiguration;
import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.common.auth.SocialProvider;
import com.sungho.letterpick.common.domain.Email;
import com.sungho.letterpick.common.outbox.OutboxMessage;
import com.sungho.letterpick.common.outbox.OutboxMessageRepository;
import com.sungho.letterpick.common.outbox.OutboxMessageStatus;
import com.sungho.letterpick.common.outbox.OutboxMessageType;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.Nickname;
import com.sungho.letterpick.member.domain.NewsletterInboxAddress;
import com.sungho.letterpick.member.domain.SocialIdentity;
import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.PublicIssueViewCountRepository;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import com.sungho.letterpick.newsletter.domain.PublicIssueViewCount;
import com.sungho.letterpick.support.database.CleanDatabase;
import com.sungho.letterpick.support.time.MutableClock;
import com.sungho.letterpick.support.time.MutableClockTestConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import({
        LetterPickTestConfiguration.class,
        LetterPickRedisTestConfiguration.class,
        MutableClockTestConfiguration.class
})
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "letterpick.outbox.publish.enabled=false",
        "letterpick.outbox.retry.enabled=false",
        "letterpick.mail.sqs-listener.enabled=false",
        "letterpick.public-issue.view-count.snapshot-interval=1",
        "letterpick.outbox.queue.trending-lifecycle-events=letterpick-test-trending-lifecycle-events",
        "letterpick.outbox.queue.trending-score-events=letterpick-test-trending-score-events",
        "newsletter.public-feed.collector-inbox-address=abcd1234efgh@inbound.letterpick.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@CleanDatabase
class PublicNewsletterIssueViewCountRecordFlowIntegrationTest {

    private static final String COLLECTOR_INBOX_ADDRESS =
            "abcd1234efgh@inbound.letterpick.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private InboundEmailRepository inboundEmailRepository;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private PublicIssueViewCountRepository publicIssueViewCountRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.setInstant("2050-06-01T00:00:00Z");
        redisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("POST 조회수 기록 요청은 Redis counter를 증가시키고 RDS backup과 ISSUE_VIEW_COUNT_UPDATED outbox 메시지를 저장한다")
    void recordIssueView_recordsBackupAndOutboxMessage() throws Exception {
        // given
        NewsletterIssue issue = savePublicIssue();

        // when & then
        mockMvc.perform(post("/api/v1/newsletter-issues/{issueId}/views", issue.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        PublicIssueViewCount viewCount = publicIssueViewCountRepository.findById(issue.getId()).orElseThrow();
        assertThat(viewCount.getIssueId()).isEqualTo(issue.getId());
        assertThat(viewCount.getViewCount()).isEqualTo(1L);
        assertThat(viewCount.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-01T00:00:00Z"));

        List<OutboxMessage> messages = outboxMessageRepository.findAll().stream()
                .filter(candidate -> candidate.getEventType().equals(OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED.eventType()))
                .filter(candidate -> candidate.getAggregateId().equals(String.valueOf(issue.getId())))
                .toList();
        assertThat(messages).hasSize(1);

        OutboxMessage message = messages.getFirst();
        assertThat(message.getEventId()).isNotBlank();
        assertThat(message.getDestination()).isEqualTo("letterpick-test-trending-score-events");
        assertThat(message.getEventType()).isEqualTo(OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED.eventType());
        assertThat(message.getSchemaVersion()).isEqualTo(OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED.schemaVersion());
        assertThat(message.getSource()).isEqualTo("letterpick");
        assertThat(message.getAggregateType()).isEqualTo(OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED.aggregateType());
        assertThat(message.getAggregateId()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);

        JsonNode payload = objectMapper.readTree(message.getPayload());
        assertThat(payload.path("issueId").asText()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(payload.path("viewCount").asText()).isEqualTo("1");
    }

    private NewsletterIssue savePublicIssue() {
        Member collector = memberRepository.save(Member.register(
                new Email("collector@example.com"),
                new Nickname("collector"),
                new SocialIdentity(SocialProvider.GOOGLE, "collector-google-id"),
                new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS)
        ));
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("테스트 뉴스레터", NewsletterCategory.TECH)
        );
        InboundEmail inboundEmail = inboundEmailRepository.save(InboundEmail.create(
                "message-view-count-flow-1",
                "s3://bucket/raw.eml",
                collector.getNewsletterInboxAddress().address(),
                newsletter.getEmail().address(),
                "테스트 이슈",
                Instant.parse("2050-06-01T00:00:00Z")
        ));
        return newsletterIssueRepository.save(NewsletterIssue.create(
                collector.getId(),
                newsletter.getId(),
                inboundEmail.getId(),
                "테스트 이슈",
                "<p>본문</p>",
                "미리보기",
                Instant.parse("2050-06-01T00:00:00Z")
        ));
    }
}
