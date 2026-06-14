package com.sungho.letterpick.newsletter.application;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountSnapshotRecorder;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import com.sungho.letterpick.newsletter.domain.PublicIssueViewCount;
import com.sungho.letterpick.support.time.MutableClock;
import com.sungho.letterpick.support.time.MutableClockTestConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import({
        LetterPickTestConfiguration.class,
        MutableClockTestConfiguration.class
})
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "letterpick.outbox.publish.enabled=false",
        "letterpick.mail.sqs-listener.enabled=false",
        "letterpick.outbox.queue.trending-lifecycle-events=letterpick-test-trending-lifecycle-events",
        "letterpick.outbox.queue.trending-score-events=letterpick-test-trending-score-events"
})
@ActiveProfiles("test")
@Transactional
class PublicIssueViewCountSnapshotRecorderIntegrationTest {

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
    private PublicIssueViewCountSnapshotRecorder snapshotRecorder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        clock.setInstant(Instant.parse("2050-06-01T00:00:00Z"));
    }

    @Test
    @DisplayName("조회수 snapshot을 RDS backup에 저장하고 ISSUE_VIEW_COUNT_UPDATED outbox 메시지를 기록한다")
    void recordSnapshot_recordsBackupAndOutboxMessage() throws Exception {
        // given
        NewsletterIssue issue = saveIssue();

        // when
        snapshotRecorder.recordSnapshot(issue.getId(), 50L);

        // then
        PublicIssueViewCount snapshot = publicIssueViewCountRepository.findById(issue.getId()).orElseThrow();
        assertThat(snapshot.getIssueId()).isEqualTo(issue.getId());
        assertThat(snapshot.getViewCount()).isEqualTo(50L);
        assertThat(snapshot.getUpdatedAt()).isNotNull();

        List<OutboxMessage> messages = outboxMessageRepository.findAll();
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
        assertThat(payload.path("viewCount").asText()).isEqualTo("50");
    }

    @Test
    @DisplayName("같은 이슈의 다음 조회수 snapshot은 RDS backup row를 갱신하고 outbox 메시지를 추가한다")
    void recordSnapshot_updatesBackupAndAddsOutboxMessage() {
        // given
        NewsletterIssue issue = saveIssue();
        snapshotRecorder.recordSnapshot(issue.getId(), 50L);

        // when
        snapshotRecorder.recordSnapshot(issue.getId(), 100L);

        // then
        PublicIssueViewCount snapshot = publicIssueViewCountRepository.findById(issue.getId()).orElseThrow();
        assertThat(snapshot.getViewCount()).isEqualTo(100L);
        assertThat(publicIssueViewCountRepository.findAll()).hasSize(1);
        assertThat(outboxMessageRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("낮은 조회수 snapshot이 나중에 도착해도 RDS backup 조회수는 감소하지 않고 outbox 메시지를 추가하지 않는다")
    void recordSnapshot_doesNotDecreaseBackupViewCountOrAddOutboxMessage() throws Exception {
        // given
        NewsletterIssue issue = saveIssue();
        clock.setInstant(Instant.parse("2050-06-01T00:00:00Z"));
        snapshotRecorder.recordSnapshot(issue.getId(), 100L);
        Instant firstUpdatedAt = publicIssueViewCountRepository.findById(issue.getId())
                .orElseThrow()
                .getUpdatedAt();

        // when
        clock.setInstant(Instant.parse("2050-06-01T00:05:00Z"));
        snapshotRecorder.recordSnapshot(issue.getId(), 50L);
        entityManager.flush();
        entityManager.clear();

        // then
        PublicIssueViewCount snapshot = publicIssueViewCountRepository.findById(issue.getId()).orElseThrow();
        assertThat(snapshot.getViewCount()).isEqualTo(100L);
        assertThat(snapshot.getUpdatedAt()).isEqualTo(firstUpdatedAt);

        List<OutboxMessage> messages = outboxMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        JsonNode payload = objectMapper.readTree(messages.getFirst().getPayload());
        assertThat(payload.path("viewCount").asText()).isEqualTo("100");
    }

    private NewsletterIssue saveIssue() {
        Member member = memberRepository.save(Member.register(
                new Email("collector@example.com"),
                new Nickname("collector"),
                new SocialIdentity(SocialProvider.GOOGLE, "collector-google-id"),
                new NewsletterInboxAddress("c00000000001@inbound.letterpick.test")
        ));
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("테스트 뉴스레터", NewsletterCategory.TECH)
        );
        InboundEmail inboundEmail = inboundEmailRepository.save(InboundEmail.create(
                "message-1",
                "s3://bucket/raw.eml",
                member.getNewsletterInboxAddress().address(),
                newsletter.getEmail().address(),
                "테스트 이슈",
                Instant.parse("2050-06-01T00:00:00Z")
        ));
        return newsletterIssueRepository.save(NewsletterIssue.create(
                member.getId(),
                newsletter.getId(),
                inboundEmail.getId(),
                "테스트 이슈",
                "<p>본문</p>",
                "미리보기",
                Instant.parse("2050-06-01T00:00:00Z")
        ));
    }

}
