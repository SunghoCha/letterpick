package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({
        LetterPickTestConfiguration.class,
        NewsletterIssueQueryService.class,
        NewsletterIssueQueryServiceIntegrationTest.ClockTestConfig.class
})
class NewsletterIssueQueryServiceIntegrationTest {

    @Autowired
    private NewsletterIssueQueryService newsletterIssueQueryService;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상세 조회하면 이슈를 읽음 처리하고 변경 후 상세 정보를 반환한다")
    void readIssueDetail_marks_issue_read_and_returns_detail_after_read() {
        // given
        Long memberId = 1L;
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("상세 뉴스레터", NewsletterCategory.TECH)
        );
        NewsletterIssue newsletterIssue = newsletterIssueRepository.save(
                NewsletterIssue.create(
                        memberId,
                        newsletter.getId(),
                        100L,
                        "상세 이슈",
                        "상세 본문",
                        "상세 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z")
                )
        );

        entityManager.flush();
        entityManager.clear();

        // when
        NewsletterIssueDetail detail = newsletterIssueQueryService.readIssueDetail(memberId, newsletterIssue.getId());

        // then
        assertThat(detail.issueId()).isEqualTo(newsletterIssue.getId());
        assertThat(detail.newsletterId()).isEqualTo(newsletter.getId());
        assertThat(detail.newsletterName()).isEqualTo(newsletter.getName());
        assertThat(detail.newsletterImageUrl()).isEqualTo(newsletter.getImageUrl());
        assertThat(detail.subject()).isEqualTo("상세 이슈");
        assertThat(detail.content()).isEqualTo("상세 본문");
        assertThat(detail.receivedAt()).isEqualTo(Instant.parse("2050-05-12T01:00:00Z"));
        assertThat(detail.read()).isTrue();

        entityManager.flush();
        entityManager.clear();

        NewsletterIssue foundIssue = newsletterIssueRepository.findById(newsletterIssue.getId()).orElseThrow();
        assertThat(foundIssue.isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이슈를 상세 조회하면 예외가 발생한다")
    void readIssueDetail_throws_when_issue_not_found() {
        // given
        Long memberId = 1L;
        Long unknownIssueId = 999L;

        // then
        assertThatThrownBy(() -> newsletterIssueQueryService.readIssueDetail(memberId, unknownIssueId))
                .isInstanceOf(NewsletterIssueNotFoundException.class);
    }

    @Test
    @DisplayName("다른 회원의 이슈를 상세 조회하면 예외가 발생하고 읽음 처리하지 않는다")
    void readIssueDetail_throws_and_does_not_mark_read_when_issue_belongs_to_other_member() {
        // given
        Long ownerMemberId = 1L;
        Long otherMemberId = 2L;
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("상세 뉴스레터", NewsletterCategory.TECH)
        );
        NewsletterIssue newsletterIssue = newsletterIssueRepository.save(
                NewsletterIssue.create(
                        ownerMemberId,
                        newsletter.getId(),
                        200L,
                        "다른 회원 이슈",
                        "다른 회원 본문",
                        "다른 회원 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z")
                )
        );

        entityManager.flush();
        entityManager.clear();

        // then
        assertThatThrownBy(() -> newsletterIssueQueryService.readIssueDetail(otherMemberId, newsletterIssue.getId()))
                .isInstanceOf(NewsletterIssueNotFoundException.class);

        NewsletterIssue foundIssue = newsletterIssueRepository.findById(newsletterIssue.getId()).orElseThrow();
        assertThat(foundIssue.isRead()).isFalse();
    }

    @Test
    @DisplayName("삭제된 이슈를 상세 조회하면 예외가 발생하고 읽음 처리하지 않는다")
    void readIssueDetail_throws_and_does_not_mark_read_when_issue_is_deleted() {
        // given
        Long memberId = 1L;
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("상세 뉴스레터", NewsletterCategory.TECH)
        );
        NewsletterIssue newsletterIssue = NewsletterIssue.create(
                memberId,
                newsletter.getId(),
                300L,
                "삭제된 이슈",
                "삭제된 본문",
                "삭제된 미리보기",
                Instant.parse("2050-05-12T01:00:00Z")
        );
        newsletterIssue.deleteFromList();
        newsletterIssueRepository.save(newsletterIssue);

        entityManager.flush();
        entityManager.clear();

        // then
        assertThatThrownBy(() -> newsletterIssueQueryService.readIssueDetail(memberId, newsletterIssue.getId()))
                .isInstanceOf(NewsletterIssueNotFoundException.class);

        NewsletterIssue foundIssue = newsletterIssueRepository.findById(newsletterIssue.getId()).orElseThrow();
        assertThat(foundIssue.isRead()).isFalse();
    }

    @TestConfiguration
    static class ClockTestConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2050-05-12T03:00:00Z"), ZoneOffset.UTC);
        }
    }
}
