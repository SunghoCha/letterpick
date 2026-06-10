package com.sungho.letterpick.trending.publicissue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "public_issue_candidate",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_public_issue_candidate_issue_id", columnNames = "issue_id")
        },
        indexes = {
                @Index(name = "idx_public_issue_candidate_status_collected",
                        columnList = "status, public_feed_collected_at")
        })
public class PublicIssueCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "newsletter_id")
    private Long newsletterId;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PublicIssueCandidateStatus status;

    @Column(name = "public_feed_collected_at")
    private Instant publicFeedCollectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private PublicIssueCandidate(Long issueId, Long newsletterId, String category,
                                 Instant publicFeedCollectedAt, Instant now) {
        this.issueId = requireNonNull(issueId);
        this.newsletterId = requireNonNull(newsletterId);
        this.category = requireNonNull(category);
        this.status = PublicIssueCandidateStatus.AVAILABLE;
        this.publicFeedCollectedAt = requireNonNull(publicFeedCollectedAt);
        this.createdAt = requireNonNull(now);
        this.updatedAt = now;
    }

    public static PublicIssueCandidate available(Long issueId, Long newsletterId, String category,
                                                 Instant publicFeedCollectedAt, Instant now) {
        return new PublicIssueCandidate(issueId, newsletterId, category, publicFeedCollectedAt, now);
    }
}
