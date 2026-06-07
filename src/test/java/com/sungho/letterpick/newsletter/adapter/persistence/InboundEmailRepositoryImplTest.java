package com.sungho.letterpick.newsletter.adapter.persistence;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.INVALID_RECIPIENT_ADDRESS;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.NEWSLETTER_NOT_FOUND;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECIPIENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.member.adapter.persistence.MemberRepository;
import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberFixture;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
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
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

@LetterPickDataJpaTest
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
        // given
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

        // when
        List<InboundEmailStatusCount> result = inboundEmailRepository.countByStatus(receivedFrom, receivedTo);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                new InboundEmailStatusCount(ISSUE_CREATED, 2L),
                new InboundEmailStatusCount(RECIPIENT_NOT_FOUND, 1L),
                new InboundEmailStatusCount(RECEIVED, 1L)
        );
    }

    @Test
    @DisplayName("상태별 집계 조회 시 수신 시각 범위가 올바르지 않으면 예외가 발생한다")
    void countByStatus_throws_exception_when_received_at_range_is_invalid() {
        // given
        Instant receivedAt = Instant.parse("2050-05-12T15:00:00Z");

        // when & then
        assertThatThrownBy(() -> inboundEmailRepository.countByStatus(receivedAt, receivedAt))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("receivedFrom must be before receivedTo");
    }

    @Test
    @DisplayName("수신 시각 범위 안의 조치 필요 인입 메일을 최신순으로 조회한다")
    void findActionRequired_returns_recent_action_required_inbound_emails() {
        // given
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        Member member = memberRepository.save(MemberFixture.createMember());
        Newsletter newsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());

        InboundEmail newsletterNotFound = createInboundEmail("newsletter-not-found", Instant.parse("2050-05-12T14:00:00Z"));
        newsletterNotFound.markNewsletterNotFound(member.getId());

        InboundEmail invalidRecipientAddress = createInboundEmail("invalid-recipient-address", Instant.parse("2050-05-12T13:00:00Z"));
        invalidRecipientAddress.markInvalidRecipientAddress();

        InboundEmail recipientNotFound = createInboundEmail("recipient-not-found-action", Instant.parse("2050-05-12T12:00:00Z"));
        recipientNotFound.markRecipientNotFound();

        InboundEmail issueCreated = createInboundEmail("issue-created-action", Instant.parse("2050-05-12T11:00:00Z"));
        issueCreated.markIssueCreated(member.getId(), newsletter.getId());

        InboundEmail received = createInboundEmail("received-action", Instant.parse("2050-05-12T10:00:00Z"));

        InboundEmail beforeRange = createInboundEmail("before-range-action", receivedFrom.minusSeconds(1));
        beforeRange.markRecipientNotFound();

        InboundEmail atReceivedTo = createInboundEmail("at-received-to-action", receivedTo);
        atReceivedTo.markInvalidRecipientAddress();

        inboundEmailRepository.saveAll(List.of(
                newsletterNotFound,
                invalidRecipientAddress,
                recipientNotFound,
                issueCreated,
                received,
                beforeRange,
                atReceivedTo
        ));
        entityManager.flush();
        entityManager.clear();

        // when
        Slice<InboundEmailAdminItem> result = inboundEmailRepository.findActionRequired(
                receivedFrom,
                receivedTo,
                PageRequest.of(0, 2)
        );

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent())
                .extracting(InboundEmailAdminItem::status)
                .containsExactly(NEWSLETTER_NOT_FOUND, INVALID_RECIPIENT_ADDRESS);
        assertThat(result.getContent())
                .extracting(InboundEmailAdminItem::messageKey)
                .containsExactly("newsletter-not-found", "invalid-recipient-address");
        assertThat(result.getContent().getFirst().memberId()).isEqualTo(member.getId());
        assertThat(result.getContent().getFirst().newsletterId()).isNull();
    }

    @Test
    @DisplayName("조치 필요 목록 조회 시 수신 시각 범위가 올바르지 않으면 예외가 발생한다")
    void findActionRequired_throws_exception_when_received_at_range_is_invalid() {
        // given
        Instant receivedAt = Instant.parse("2050-05-12T15:00:00Z");

        // when & then
        assertThatThrownBy(() -> inboundEmailRepository.findActionRequired(
                receivedAt,
                receivedAt,
                PageRequest.of(0, 20)
        ))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("receivedFrom must be before receivedTo");
    }

    @Test
    @DisplayName("기준 시각 이전 RECEIVED 인입 메일을 오래된 순으로 조회한다")
    void findStaleReceived_returns_received_inbound_emails_before_cutoff_ordered_oldest_first() {
        // given
        Instant receivedBefore = Instant.parse("2050-05-12T15:00:00Z");

        Member member = memberRepository.save(MemberFixture.createMember());
        Newsletter newsletter = newslettersRepository.save(NewsletterFixture.createNewsletter());

        InboundEmail oldestReceived = createInboundEmail("stale-oldest", Instant.parse("2050-05-12T10:00:00Z"));
        InboundEmail sameTimeFirst = createInboundEmail("stale-same-time-first", Instant.parse("2050-05-12T11:00:00Z"));
        InboundEmail sameTimeSecond = createInboundEmail("stale-same-time-second", Instant.parse("2050-05-12T11:00:00Z"));
        InboundEmail latestStaleReceived = createInboundEmail("stale-later", Instant.parse("2050-05-12T12:00:00Z"));
        InboundEmail atCutoff = createInboundEmail("at-cutoff", receivedBefore);
        InboundEmail afterCutoff = createInboundEmail("after-cutoff", receivedBefore.plusSeconds(1));

        InboundEmail issueCreated = createInboundEmail("stale-issue-created", Instant.parse("2050-05-12T09:00:00Z"));
        issueCreated.markIssueCreated(member.getId(), newsletter.getId());

        InboundEmail recipientNotFound = createInboundEmail("stale-recipient-not-found", Instant.parse("2050-05-12T08:00:00Z"));
        recipientNotFound.markRecipientNotFound();

        inboundEmailRepository.saveAll(List.of(
                oldestReceived,
                sameTimeFirst,
                sameTimeSecond,
                latestStaleReceived,
                atCutoff,
                afterCutoff,
                issueCreated,
                recipientNotFound
        ));
        entityManager.flush();
        entityManager.clear();

        // when
        Slice<InboundEmailAdminItem> result = inboundEmailRepository.findStaleReceived(
                receivedBefore,
                PageRequest.of(0, 3)
        );

        // then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent())
                .extracting(InboundEmailAdminItem::status)
                .containsExactly(RECEIVED, RECEIVED, RECEIVED);
        assertThat(result.getContent())
                .extracting(InboundEmailAdminItem::messageKey)
                .containsExactly("stale-oldest", "stale-same-time-first", "stale-same-time-second");
        assertThat(result.getContent().getFirst().memberId()).isNull();
        assertThat(result.getContent().getFirst().newsletterId()).isNull();

        // when
        Slice<InboundEmailAdminItem> allResult = inboundEmailRepository.findStaleReceived(
                receivedBefore,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(allResult.hasNext()).isFalse();
        assertThat(allResult.getContent())
                .extracting(InboundEmailAdminItem::messageKey)
                .containsExactly(
                        "stale-oldest",
                        "stale-same-time-first",
                        "stale-same-time-second",
                        "stale-later"
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
