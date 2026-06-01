package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import com.sungho.letterpick.support.database.CleanDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@CleanDatabase
class NewsletterIssueRepositoryImplFullTextTest {

    @Autowired
    NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    NewslettersRepository newslettersRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                    AND table_name = 'newsletter_issue'
                    AND index_name = 'ft_newsletter_issue_subject_content_ngram'
                """, Integer.class);

        if (indexCount == null || indexCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE newsletter_issue
                        ADD FULLTEXT INDEX ft_newsletter_issue_subject_content_ngram (subject, content)
                            WITH PARSER ngram
                    """);
        }
    }

    @Test
    @DisplayName("FULLTEXT 공개 피드 검색은 제목과 본문을 대상으로 하고 카테고리 조건과 함께 최신순으로 조회한다")
    void findPublicIssuesByMemberIdWithFullTextSearchesSubjectAndContentWithCategoryFilter() {
        // given
        Long collectorMemberId = 1L;
        Long otherMemberId = 2L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("검색 테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter keywordNameNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("검색 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("검색 비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        NewsletterIssue subjectMatchedIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 1L, "redis 운영 사례",
                        "다른 본문", "제목 매칭 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue contentMatchedIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 2L, "다른 제목",
                        "본문에서 redis 캐시 전략을 다룬다", "본문 매칭 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, keywordNameNewsletter.getId(), 3L, "다른 제목",
                        "다른 본문", "뉴스레터 이름만 매칭 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 4L, "redis 비즈 이슈",
                        "다른 본문", "다른 카테고리 미리보기", Instant.parse("2050-05-12T04:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, techNewsletter.getId(), 5L, "redis 다른 회원 이슈",
                        "다른 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T05:00:00Z"))
        );
        NewsletterIssue deletedIssue = createIssue(collectorMemberId, techNewsletter.getId(), 6L, "redis 삭제 이슈",
                "다른 본문", "삭제 이슈 미리보기", Instant.parse("2050-05-12T06:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberIdWithFullTextRaw(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.TECH, "redis"),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(contentMatchedIssue.getId(), subjectMatchedIssue.getId());
    }

    @Test
    @DisplayName("FULLTEXT all_terms 공개 피드 조회는 검색어 없이 카테고리만 있어도 조회한다")
    void findPublicIssuesByMemberIdWithFullTextAllTermsFiltersByCategoryWithoutKeyword() {
        // given
        Long collectorMemberId = 1L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("기술 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 1L, "기술 이슈",
                        "기술 본문", "기술 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue bizIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 2L, "비즈 이슈",
                        "비즈 본문", "비즈 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberIdWithFullTextAllTerms(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.BIZ, null),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().issueId()).isEqualTo(bizIssue.getId());
    }

    @Test
    @DisplayName("FULLTEXT raw 공개 피드 조회는 검색어 없이 카테고리만 있어도 조회한다")
    void findPublicIssuesByMemberIdWithFullTextRawFiltersByCategoryWithoutKeyword() {
        // given
        Long collectorMemberId = 1L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("기술 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 1L, "기술 이슈",
                        "기술 본문", "기술 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue bizIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 2L, "비즈 이슈",
                        "비즈 본문", "비즈 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberIdWithFullTextRaw(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.BIZ, null),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().issueId()).isEqualTo(bizIssue.getId());
    }

    private NewsletterIssue createIssue(Long memberId, Long newsletterId, Long inboundEmailId,
                                        String subject, String content, String previewText, Instant receivedAt) {
        return NewsletterIssue.create(memberId, newsletterId, inboundEmailId,
                                      subject, content, previewText, receivedAt);
    }
}
