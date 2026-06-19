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
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import(LetterPickTestConfiguration.class)
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "letterpick.outbox.publish.enabled=false",
        "letterpick.mail.sqs-listener.enabled=false",
        "newsletter.public-feed.collector-inbox-address=abcd1234efgh@inbound.letterpick.test"
})
@ActiveProfiles("test")
class PublicIssueRemovedOutboxIntegrationTest {

    private static final String COLLECTOR_INBOX_ADDRESS =
            "abcd1234efgh@inbound.letterpick.test";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private PublicNewsletterIssueModifier publicNewsletterIssueModifier;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("공개 피드 이슈를 삭제하면 PUBLIC_ISSUE_REMOVED outbox 메시지가 저장된다")
    void deletePublicIssue_recordsPublicIssueRemovedOutboxMessage() throws Exception {
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

        NewsletterIssue issue = newsletterIssueRepository.save(NewsletterIssue.create(
                collector.getId(),
                newsletter.getId(),
                1L,
                "삭제 대상 공개 이슈",
                "content",
                "preview",
                Instant.parse("2050-06-10T00:00:00Z")
        ));

        // when
        publicNewsletterIssueModifier.delete(issue.getId());

        // then
        NewsletterIssue deletedIssue = newsletterIssueRepository.findById(issue.getId()).orElseThrow();
        assertThat(deletedIssue.isDeleted()).isTrue();

        OutboxMessage message = outboxMessageRepository.findAll()
                .stream()
                .filter(candidate -> candidate.getEventType().equals(OutboxMessageType.PUBLIC_ISSUE_REMOVED.eventType()))
                .filter(candidate -> candidate.getAggregateId().equals(String.valueOf(issue.getId())))
                .findFirst()
                .orElseThrow();

        assertThat(message.getEventId()).isNotBlank();
        assertThat(message.getDestination()).isEqualTo("letterpick-test-trending-lifecycle-events");
        assertThat(message.getEventType()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_REMOVED.eventType());
        assertThat(message.getSchemaVersion()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_REMOVED.schemaVersion());
        assertThat(message.getSource()).isEqualTo("letterpick");
        assertThat(message.getAggregateType()).isEqualTo(OutboxMessageType.PUBLIC_ISSUE_REMOVED.aggregateType());
        assertThat(message.getAggregateId()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);

        JsonNode payload = objectMapper.readTree(message.getPayload());
        assertThat(payload.path("issueId").asText()).isEqualTo(String.valueOf(issue.getId()));
        assertThat(payload.path("publicFeedCollectedAt").asText()).isEqualTo(issue.getReceivedAt().toString());
    }
}
