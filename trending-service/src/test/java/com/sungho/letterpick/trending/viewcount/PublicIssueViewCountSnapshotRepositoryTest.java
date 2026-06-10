package com.sungho.letterpick.trending.viewcount;

import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TrendingServiceTestConfiguration.class)
@ActiveProfiles("test")
class PublicIssueViewCountSnapshotRepositoryTest {

    @Autowired
    private PublicIssueViewCountSnapshotRepository repository;

    @Test
    @DisplayName("조회수 snapshot을 저장하고 issueId로 조회한다")
    void save_and_find_by_issue_id() {
        // given
        PublicIssueViewCountSnapshot snapshot = PublicIssueViewCountSnapshot.record(
                1L,
                150L,
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // when
        repository.save(snapshot);

        // then
        PublicIssueViewCountSnapshot found = repository.findById(1L).orElseThrow();
        assertThat(found.getIssueId()).isEqualTo(1L);
        assertThat(found.getViewCount()).isEqualTo(150L);
        assertThat(found.getSnapshotOccurredAt()).isEqualTo(Instant.parse("2050-06-10T00:59:00Z"));
        assertThat(found.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(found.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
    }

    @Test
    @DisplayName("조회수 snapshot이 없으면 새로 저장한다")
    void upsertSnapshot_inserts_when_snapshot_is_absent() {
        // when
        repository.upsertSnapshot(
                10L,
                150L,
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // then
        PublicIssueViewCountSnapshot snapshot = repository.findById(10L).orElseThrow();
        assertThat(snapshot.getIssueId()).isEqualTo(10L);
        assertThat(snapshot.getViewCount()).isEqualTo(150L);
        assertThat(snapshot.getSnapshotOccurredAt()).isEqualTo(Instant.parse("2050-06-10T00:59:00Z"));
        assertThat(snapshot.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(snapshot.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
    }

    @Test
    @DisplayName("더 높은 조회수 snapshot이면 기존 snapshot을 갱신한다")
    void upsertSnapshot_updates_when_view_count_increases() {
        // given
        repository.upsertSnapshot(
                10L,
                100L,
                Instant.parse("2050-06-10T00:59:00Z"),
                Instant.parse("2050-06-10T01:00:00Z")
        );

        // when
        repository.upsertSnapshot(
                10L,
                150L,
                Instant.parse("2050-06-10T01:04:00Z"),
                Instant.parse("2050-06-10T01:05:00Z")
        );

        // then
        PublicIssueViewCountSnapshot snapshot = repository.findById(10L).orElseThrow();
        assertThat(snapshot.getViewCount()).isEqualTo(150L);
        assertThat(snapshot.getSnapshotOccurredAt()).isEqualTo(Instant.parse("2050-06-10T01:04:00Z"));
        assertThat(snapshot.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:00:00Z"));
        assertThat(snapshot.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
    }

    @Test
    @DisplayName("낮은 조회수 snapshot이 나중에 도착하면 기존 snapshot을 유지한다")
    void upsertSnapshot_ignores_lower_view_count() {
        // given
        repository.upsertSnapshot(
                10L,
                150L,
                Instant.parse("2050-06-10T01:04:00Z"),
                Instant.parse("2050-06-10T01:05:00Z")
        );

        // when
        repository.upsertSnapshot(
                10L,
                100L,
                Instant.parse("2050-06-10T01:09:00Z"),
                Instant.parse("2050-06-10T01:10:00Z")
        );

        // then
        PublicIssueViewCountSnapshot snapshot = repository.findById(10L).orElseThrow();
        assertThat(snapshot.getViewCount()).isEqualTo(150L);
        assertThat(snapshot.getSnapshotOccurredAt()).isEqualTo(Instant.parse("2050-06-10T01:04:00Z"));
        assertThat(snapshot.getCreatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
        assertThat(snapshot.getUpdatedAt()).isEqualTo(Instant.parse("2050-06-10T01:05:00Z"));
    }
}
