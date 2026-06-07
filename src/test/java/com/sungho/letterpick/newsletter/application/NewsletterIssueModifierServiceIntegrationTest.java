package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.adapter.persistence.NewslettersRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({
        LetterPickTestConfiguration.class,
        NewsletterIssueModifierService.class
})
class NewsletterIssueModifierServiceIntegrationTest {

    @Autowired
    private NewsletterIssueModifierService newsletterIssueModifierService;

    @Autowired
    private NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    private NewslettersRepository newslettersRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("자신의 뉴스레터 이슈를 삭제하면 목록에서 제외되도록 삭제 처리된다")
    void delete_marks_own_issue_deleted_and_excludes_it_from_user_lookup() {
        // given
        Long memberId = 1L;
        NewsletterIssue newsletterIssue = saveNewsletterIssue(memberId);
        Long issueId = newsletterIssue.getId();

        em.flush();
        em.clear();

        // when
        newsletterIssueModifierService.delete(memberId, issueId);
        em.flush();
        em.clear();

        // then
        NewsletterIssue found = newsletterIssueRepository.findById(issueId).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
        assertThat(newsletterIssueRepository.findByIdAndMemberIdAndDeletedFalse(issueId, memberId))
                .isEmpty();
    }

    @Test
    @DisplayName("다른 회원의 뉴스레터 이슈를 삭제 시도하면 예외가 발생한다")
    void delete_throws_and_does_not_delete_when_issue_belongs_to_other_member() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;
        NewsletterIssue newsletterIssue = saveNewsletterIssue(otherMemberId);
        em.flush();
        em.clear();

        // then
        assertThatThrownBy(() -> newsletterIssueModifierService.delete(memberId, newsletterIssue.getId()))
                .isInstanceOf(NewsletterIssueNotFoundException.class);

        em.flush();
        em.clear();

        NewsletterIssue found = newsletterIssueRepository.findById(newsletterIssue.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 뉴스레터 이슈를 다시 삭제하면 예외가 발생한다")
    void delete_throws_when_issue_is_already_deleted() {
        // given
        Long memberId = 1L;
        NewsletterIssue newsletterIssue = saveNewsletterIssue(memberId);
        newsletterIssue.deleteFromList();
        em.flush();
        em.clear();

        // then
        assertThatThrownBy(() -> newsletterIssueModifierService.delete(memberId, newsletterIssue.getId()))
                .isInstanceOf(NewsletterIssueNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 뉴스레터 이슈를 삭제하면 예외가 발생한다")
    void delete_throws_when_issue_not_found() {
        // given
        Long memberId = 1L;
        Long wrongIssueId = 999L;

        // then
        assertThatThrownBy(() -> newsletterIssueModifierService.delete(memberId, wrongIssueId))
                .isInstanceOf(NewsletterIssueNotFoundException.class);
    }

    private NewsletterIssue saveNewsletterIssue(Long memberId) {
        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("삭제 뉴스레터", NewsletterCategory.TECH)
        );
        return newsletterIssueRepository.save(
                NewsletterIssue.create(
                        memberId,
                        newsletter.getId(),
                        100L,
                        "삭제할 이슈",
                        "삭제할 본문",
                        "삭제할 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z")
                )
        );
    }
}
