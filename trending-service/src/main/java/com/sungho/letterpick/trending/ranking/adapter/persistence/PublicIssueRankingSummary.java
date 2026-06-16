package com.sungho.letterpick.trending.ranking.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "public_issue_ranking_summary")
@IdClass(PublicIssueRankingSummaryId.class)
public class PublicIssueRankingSummary {

    @Id
    @Column(name = "window_type", nullable = false, length = 20)
    private String windowType;

    @Id
    @Column(name = "window_key", nullable = false, length = 20)
    private String windowKey;

    @Id
    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(nullable = false)
    private long score;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
