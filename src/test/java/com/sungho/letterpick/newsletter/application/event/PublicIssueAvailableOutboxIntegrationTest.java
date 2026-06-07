package com.sungho.letterpick.newsletter.application.event;

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
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.NewsletterMailReceiveService;
import com.sungho.letterpick.newsletter.application.ReceivedMail;
import com.sungho.letterpick.newsletter.application.ReceivedMailFixture;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(LetterPickTestConfiguration.class)
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "letterpick.outbox.publish.enabled=false",
        "letterpick.outbox.retry.enabled=false",
        "letterpick.mail.sqs-listener.enabled=false",
        "letterpick.outbox.queue.trending-lifecycle-events=letterpick-test-trending-lifecycle-events",
        "newsletter.public-feed.collector-inbox-address=abcd1234efgh@inbound.letterpick.test"
})
@ActiveProfiles("test")
class PublicIssueAvailableOutboxIntegrationTest {

    private static final String COLLECTOR_INBOX_ADDRESS =
            "abcd1234efgh@inbound.letterpick.test";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private NewsletterMailReceiveService newsletterMailReceiveService;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("공개 피드 수집 메일이 이슈를 생성하면 PUBLIC_ISSUE_AVAILABLE outbox 메시지가 저장된다")
    void receiveCollectorMail_recordsPublicIssueAvailableOutboxMessage() throws Exception {
        // given
        Member collector = memberRepository.save(Member.register(
                new Email("collector@example.com"),
                new Nickname("collector"),
                new SocialIdentity(SocialProvider.GOOGLE, "collector-google-id"),
                new NewsletterInboxAddress(COLLECTOR_INBOX_ADDRESS)
        ));

        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("테스트 뉴스레터", NewsletterCategory.TECH)
        );

        ReceivedMail receivedMail = ReceivedMailFixture.create(
                "message-public-issue-available-1",
                COLLECTOR_INBOX_ADDRESS,
                newsletter.getEmail().address()
        );

        // when
        newsletterMailReceiveService.receive(receivedMail);
        // then
        List<NewsletterIssue> issues = newsletterIssueRepository.findAll();
        assertThat(issues).hasSize(1);

        NewsletterIssue issue = issues.getFirst();
        assertThat(issue.getMemberId()).isEqualTo(collector.getId());
        assertThat(issue.getNewsletterId()).isEqualTo(newsletter.getId());

        List<OutboxMessage> messages = outboxMessageRepository.findAll();
        assertThat(messages).hasSize(1);

        OutboxMessage message = messages.getFirst();
        assertThat(message.getEventId()).isNotBlank();
        assertThat(message.getDestination()).isEqualTo("letterpick-test-trending-lifecycle-events");
        assertThat(message.getEventType()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.eventType());
        assertThat(message.getSchemaVersion()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.schemaVersion());
        assertThat(message.getSource()).isEqualTo("letterpick");
        assertThat(message.getAggregateType()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_AVAILABLE.aggregateType());
        assertThat(message.getAggregateId()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);

        JsonNode payload = objectMapper.readTree(message.getPayload());
        assertThat(payload.path("issueId").asText()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(payload.path("newsletterId").asText()).isEqualTo(String.valueOf(newsletter.getId()));
        assertThat(payload.path("category").asText()).isEqualTo(NewsletterCategory.TECH.name());
        assertThat(payload.path("publicFeedCollectedAt").asText()).isEqualTo(receivedMail.receivedAt().toString());
    }
}
