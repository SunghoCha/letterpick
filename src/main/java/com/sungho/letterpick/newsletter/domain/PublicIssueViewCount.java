package com.sungho.letterpick.newsletter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "public_issue_view_count")
public class PublicIssueViewCount {

    @Id
    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
