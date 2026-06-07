package com.sungho.letterpick.newsletter.domain;

import com.sungho.letterpick.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "newsletter_issue", uniqueConstraints = {
        @UniqueConstraint(name = "uk_newsletter_issue_inbound_email_id", columnNames = "inbound_email_id")
})
public class NewsletterIssue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "newsletter_id", nullable = false)
    private Long newsletterId;

    @Column(name = "inbound_email_id", nullable = false)
    private Long inboundEmailId;

    @Column(nullable = false, length = 512)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 120)
    private String previewText;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(name = "read_status", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private boolean deleted;

    private NewsletterIssue(Long memberId, Long newsletterId, Long inboundEmailId,
                            String subject, String content, String previewText, Instant receivedAt) {
        this.memberId = requireNonNull(memberId);
        this.newsletterId = requireNonNull(newsletterId);
        this.inboundEmailId = requireNonNull(inboundEmailId);
        this.subject = requireNonNull(subject);
        this.content = requireNonNull(content);
        this.previewText = requireNonNull(previewText);
        this.receivedAt = requireNonNull(receivedAt);
        this.read = false;
        this.deleted = false;
    }

    public static NewsletterIssue create(Long memberId, Long newsletterId, Long inboundEmailId,
                                         String subject, String content, String previewText, Instant receivedAt) {
        return new NewsletterIssue(memberId, newsletterId, inboundEmailId,
                                   subject, content, previewText, receivedAt);
    }

    public void markRead() {
        this.read = true;
    }

    public void deleteFromList() {
        this.deleted = true;
    }
}
