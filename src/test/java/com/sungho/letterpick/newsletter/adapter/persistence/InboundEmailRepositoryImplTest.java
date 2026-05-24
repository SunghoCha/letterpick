package com.sungho.letterpick.newsletter.adapter.persistence;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECIPIENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.domain.InboundEmail;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class})
class InboundEmailRepositoryImplTest {

    @Autowired
    InboundEmailRepository inboundEmailRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    NewslettersRepository newslettersRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("수신 시각 범위 안의 인입 메일을 상태별로 집계한다")
    void countByStatus_counts_inbound_emails_grouped_by_status_within_received_at_range() {
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        Member member = memberRepository.save(MemberFixture.createMember());
        Newsletter newsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());

        InboundEmail issueCreated = createInboundEmail("issue-created-1", receivedFrom);
        issueCreated.markIssueCreated(member.getId(), newsletter.getId());

        InboundEmail anotherIssueCreated = createInboundEmail("issue-created-2", Instant.parse("2050-05-12T00:00:00Z"));
        anotherIssueCreated.markIssueCreated(member.getId(), newsletter.getId());

        InboundEmail recipientNotFound = createInboundEmail("recipient-not-found", Instant.parse("2050-05-12T01:00:00Z"));
        recipientNotFound.markRecipientNotFound();

        InboundEmail received = createInboundEmail("received", Instant.parse("2050-05-12T02:00:00Z"));

        InboundEmail beforeRange = createInboundEmail("before-range", receivedFrom.minusSeconds(1));
        beforeRange.markRecipientNotFound();

        InboundEmail atReceivedTo = createInboundEmail("at-received-to", receivedTo);
        atReceivedTo.markRecipientNotFound();

        inboundEmailRepository.saveAll(List.of(
                issueCreated,
                anotherIssueCreated,
                recipientNotFound,
                received,
                beforeRange,
                atReceivedTo
        ));
        entityManager.flush();
        entityManager.clear();

        List<InboundEmailStatusCount> result = inboundEmailRepository.countByStatus(receivedFrom, receivedTo);

        assertThat(result).containsExactlyInAnyOrder(
                new InboundEmailStatusCount(ISSUE_CREATED, 2L),
                new InboundEmailStatusCount(RECIPIENT_NOT_FOUND, 1L),
                new InboundEmailStatusCount(RECEIVED, 1L)
        );
    }

    private InboundEmail createInboundEmail(String messageKey, Instant receivedAt) {
        return InboundEmail.create(
                messageKey,
                "raw/" + messageKey,
                messageKey + "@inbound.letterpick.test",
                "sender-" + messageKey + "@example.com",
                "subject " + messageKey,
                receivedAt
        );
    }
}
