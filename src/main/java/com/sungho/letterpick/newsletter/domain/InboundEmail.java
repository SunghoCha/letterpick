package com.sungho.letterpick.newsletter.domain;

import com.sungho.letterpick.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbound_email", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inbound_email_message_key", columnNames = "message_key")
})
public class InboundEmail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 255)
    private String messageKey;

    @Column(name = "raw_reference", nullable = false, length = 512)
    private String rawReference;

    @Column(name = "recipient_address", nullable = false, length = 320)
    private String recipientAddress;

    @Column(name = "sender_email", nullable = false, length = 320)
    private String senderEmail;

    @Column(nullable = false, length = 512)
    private String subject;

    @Column(nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private InboundEmailStatus status;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "newsletter_id")
    private Long newsletterId;

    private InboundEmail(String messageKey, String rawReference, String recipientAddress,
                         String senderEmail, String subject, Instant receivedAt) {
        this.messageKey = requireNonNull(messageKey);
        this.rawReference = requireNonNull(rawReference);
        this.recipientAddress = requireNonNull(recipientAddress);
        this.senderEmail = requireNonNull(senderEmail);
        this.subject = requireNonNull(subject);
        this.receivedAt = requireNonNull(receivedAt);
        this.status = InboundEmailStatus.RECEIVED;
    }

    public static InboundEmail create(String messageKey, String rawReference, String recipientAddress,
                                      String senderEmail, String subject, Instant receivedAt) {
        return new InboundEmail(messageKey, rawReference, recipientAddress, senderEmail, subject, receivedAt);
    }

    public void markIssueCreated(Long memberId, Long newsletterId) {
        this.memberId = requireNonNull(memberId);
        this.newsletterId = requireNonNull(newsletterId);
        this.status = InboundEmailStatus.ISSUE_CREATED;
    }

    public void markSkippedUnsubscribed(Long memberId, Long newsletterId) {
        this.memberId = requireNonNull(memberId);
        this.newsletterId = requireNonNull(newsletterId);
        this.status = InboundEmailStatus.SKIPPED_UNSUBSCRIBED;
    }

    public void markRecipientNotFound() {
        this.memberId = null;
        this.newsletterId = null;
        this.status = InboundEmailStatus.RECIPIENT_NOT_FOUND;
    }

    public void markInvalidRecipientAddress() {
        this.memberId = null;
        this.newsletterId = null;
        this.status = InboundEmailStatus.INVALID_RECIPIENT_ADDRESS;
    }

    public void markNewsletterNotFound(Long memberId) {
        this.memberId = requireNonNull(memberId);
        this.newsletterId = null;
        this.status = InboundEmailStatus.NEWSLETTER_NOT_FOUND;
    }
}
