package com.sungho.letterpick.trending.publicissue;

import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TrendingServiceTestConfiguration.class)
@ActiveProfiles("test")
class PublicIssueCandidateRepositoryTest {

    @Autowired
    private PublicIssueCandidateRepository repository;

    @Test
    @DisplayName("REMOVED 상태 후보가 없으면 metadata 없이 새로 저장한다")
    void upsertRemoved_inserts_removed_status_when_candidate_is_absent() {
        // when
        repository.upsertRemoved(
                10L,
                Instant.parse("2050-06-10T01:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // then
        PublicIssueCandidate candidate = repository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getIssueId()).isEqualTo(10L);
        assertThat(candidate.getNewsletterId()).isNull();
        assertThat(candidate.getCategory()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);
        assertThat(candidate.getPublicFeedCollectedAt()).isNull();
        assertThat(candidate.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(candidate.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
    }

    @Test
    @DisplayName("기존 AVAILABLE 후보가 있으면 metadata를 유지하고 REMOVED로 변경한다")
    void upsertRemoved_marks_existing_candidate_removed() {
        // given
        repository.insertAvailableIfAbsent(
                10L,
                20L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // when
        repository.upsertRemoved(
                10L,
                Instant.parse("2050-06-10T01:05:00Z"),
                Instant.parse("2050-06-10T01:05:00Z")
        );

        // then
        PublicIssueCandidate candidate = repository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isEqualTo(20L);
        assertThat(candidate.getCategory()).isEqualTo("TECH");
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);
        assertThat(candidate.getPublicFeedCollectedAt()).isEqualTo(Instant.parse("2050-06-10T00:59:00Z"));
        assertThat(candidate.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(candidate.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
    }

    @Test
    @DisplayName("기존 AVAILABLE 후보가 있으면 늦게 도착한 AVAILABLE은 metadata를 덮지 않는다")
    void insertAvailableIfAbsent_does_not_replace_existing_available_candidate() {
        // given
        repository.insertAvailableIfAbsent(
                10L,
                20L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // when
        repository.insertAvailableIfAbsent(
                10L,
                30L,
                "LIFE",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                Instant.parse("2050-06-10T01:30:00Z"),
                Instant.parse("2050-06-10T01:30:00Z"),
                Instant.parse("2050-06-10T01:30:00Z")
        );

        // then
        PublicIssueCandidate candidate = repository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isEqualTo(20L);
        assertThat(candidate.getCategory()).isEqualTo("TECH");
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.AVAILABLE);
        assertThat(candidate.getPublicFeedCollectedAt()).isEqualTo(Instant.parse("2050-06-10T00:59:00Z"));
        assertThat(candidate.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(candidate.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
    }

    @Test
    @DisplayName("REMOVED 상태 후보가 있으면 늦게 도착한 AVAILABLE은 후보를 되살리지 않는다")
    void insertAvailableIfAbsent_does_not_restore_removed_candidate() {
        // given
        repository.upsertRemoved(
                10L,
                Instant.parse("2050-06-10T01:05:00Z"),
                Instant.parse("2050-06-10T01:05:00Z")
        );

        // when
        repository.insertAvailableIfAbsent(
                10L,
                20L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:10:00Z"),
                Instant.parse("2050-06-10T01:10:00Z")
        );

        // then
        PublicIssueCandidate candidate = repository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isNull();
        assertThat(candidate.getCategory()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);
        assertThat(candidate.getPublicFeedCollectedAt()).isNull();
        assertThat(candidate.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
        assertThat(candidate.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
    }

    @Test
    @DisplayName("AVAILABLE 후보는 DB 제약으로 metadata 누락을 거부한다")
    void insertAvailableIfAbsent_rejects_available_candidate_without_metadata() {
        // when & then
        assertThatThrownBy(() -> repository.insertAvailableIfAbsent(
                10L,
                null,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
