package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.MemberNewsletter;
import com.sungho.letterpick.newsletter.domain.Newsletter;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.NewsletterFixture;
import com.sungho.letterpick.newsletter.domain.NewsletterIssue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import({LetterPickTestConfiguration.class})
class NewsletterIssueRepositoryImplTest {

    @Autowired
    NewsletterIssueRepository newsletterIssueRepository;

    @Autowired
    NewslettersRepository newslettersRepository;

    @Autowired
    MemberNewsletterRepository memberNewsletterRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("회원의 삭제되지 않은 활성 구독 이슈를 수신시각 최신순으로 조회한다")
    void findAllByMemberId_returns_active_subscription_issues_ordered_by_receivedAt_desc() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        Newsletter firstNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("첫 번째 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter secondNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("두 번째 뉴스레터", NewsletterCategory.BIZ)
        );
        Newsletter unsubscribedNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("구독 해지 뉴스레터", NewsletterCategory.TECH)
        );

        memberNewsletterRepository.save(MemberNewsletter.create(memberId, firstNewsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(memberId, secondNewsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(otherMemberId, firstNewsletter.getId()));

        MemberNewsletter unsubscribedMemberNewsletter = MemberNewsletter.create(memberId, unsubscribedNewsletter.getId());
        unsubscribedMemberNewsletter.unsubscribe();
        memberNewsletterRepository.save(unsubscribedMemberNewsletter);

        NewsletterIssue oldIssue = newsletterIssueRepository.save(
                createIssue(memberId, firstNewsletter.getId(), 1L, "오래된 이슈",
                        "오래된 본문", "오래된 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        NewsletterIssue latestIssue = newsletterIssueRepository.save(
                createIssue(memberId, secondNewsletter.getId(), 2L, "최신 이슈",
                        "최신 본문", "최신 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, firstNewsletter.getId(), 3L, "다른 회원 이슈",
                        "다른 회원 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(memberId, firstNewsletter.getId(), 4L, "범위 밖 이슈",
                        "범위 밖 본문", "범위 밖 미리보기", Instant.parse("2050-05-12T15:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(memberId, unsubscribedNewsletter.getId(), 5L, "구독 해지 이슈",
                        "구독 해지 본문", "구독 해지 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );

        NewsletterIssue deletedIssue = createIssue(memberId, firstNewsletter.getId(), 6L, "삭제된 이슈",
                "삭제된 본문", "삭제된 미리보기", Instant.parse("2050-05-12T04:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findAllByMemberId(
                memberId,
                NewsletterIssueSearchCondition.receivedAtRange(receivedFrom, receivedTo),
                PageRequest.of(0, 10)
        );
        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(latestIssue.getId(), oldIssue.getId());

        NewsletterIssueItem latestIssueItem = result.getContent().get(0);
        assertThat(latestIssueItem.newsletterId()).isEqualTo(secondNewsletter.getId());
        assertThat(latestIssueItem.newsletterName()).isEqualTo(secondNewsletter.getName());
        assertThat(latestIssueItem.newsletterImageUrl()).isEqualTo(secondNewsletter.getImageUrl());
        assertThat(latestIssueItem.subject()).isEqualTo("최신 이슈");
        assertThat(latestIssueItem.previewText()).isEqualTo("최신 미리보기");
        assertThat(latestIssueItem.receivedAt()).isEqualTo(Instant.parse("2050-05-12T01:00:00Z"));
        assertThat(latestIssueItem.read()).isFalse();
    }

    @Test
    @DisplayName("조회 결과가 페이지 크기보다 많으면 다음 페이지가 있다고 표시한다")
    void findAllByMemberId_calculates_hasNext_when_result_exceeds_page_size() {
        Long memberId = 1L;
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("테크 뉴스레터", NewsletterCategory.TECH)
        );
        memberNewsletterRepository.save(MemberNewsletter.create(memberId, newsletter.getId()));
        newsletterIssueRepository.save(
                createIssue(memberId, newsletter.getId(), 1L, "첫 번째 이슈",
                        "첫 번째 본문", "첫 번째 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(memberId, newsletter.getId(), 2L, "두 번째 이슈",
                        "두 번째 본문", "두 번째 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );

        entityManager.flush();
        entityManager.clear();

        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findAllByMemberId(
                memberId,
                NewsletterIssueSearchCondition.receivedAtRange(receivedFrom, receivedTo),
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("날짜 조건이 없으면 회원의 삭제되지 않은 활성 구독 이슈를 날짜 제한 없이 조회한다")
    void findAllByMemberId_returns_active_subscription_issues_without_received_at_range() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;

        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("보관함 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter unsubscribedNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("구독 해지 뉴스레터", NewsletterCategory.BIZ)
        );

        memberNewsletterRepository.save(MemberNewsletter.create(memberId, newsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(otherMemberId, newsletter.getId()));

        MemberNewsletter unsubscribedMemberNewsletter = MemberNewsletter.create(memberId, unsubscribedNewsletter.getId());
        unsubscribedMemberNewsletter.unsubscribe();
        memberNewsletterRepository.save(unsubscribedMemberNewsletter);

        NewsletterIssue oldIssue = newsletterIssueRepository.save(
                createIssue(memberId, newsletter.getId(), 20L, "오래된 보관함 이슈",
                        "오래된 본문", "오래된 미리보기", Instant.parse("2049-01-01T00:00:00Z"))
        );
        NewsletterIssue latestIssue = newsletterIssueRepository.save(
                createIssue(memberId, newsletter.getId(), 21L, "최신 보관함 이슈",
                        "최신 본문", "최신 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, newsletter.getId(), 22L, "다른 회원 이슈",
                        "다른 회원 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(memberId, unsubscribedNewsletter.getId(), 23L, "구독 해지 이슈",
                        "구독 해지 본문", "구독 해지 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );

        NewsletterIssue deletedIssue = createIssue(memberId, newsletter.getId(), 24L, "삭제된 이슈",
                "삭제된 본문", "삭제된 미리보기", Instant.parse("2050-05-12T04:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findAllByMemberId(
                memberId,
                NewsletterIssueSearchCondition.empty(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(latestIssue.getId(), oldIssue.getId());
    }

    @Test
    @DisplayName("키워드가 제목, 본문, 뉴스레터 이름 중 하나에 포함되면 이슈를 조회한다")
    void findAllByMemberId_returns_issues_matching_keyword_in_subject_content_or_newsletter_name() {
        // given
        Long memberId = 1L;
        String keyword = "스프링";

        Newsletter subjectNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("제목 매칭 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter contentNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("본문 매칭 뉴스레터", NewsletterCategory.BIZ)
        );
        Newsletter nameNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("스프링 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter unmatchedNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("매칭 없는 뉴스레터", NewsletterCategory.BIZ)
        );

        memberNewsletterRepository.save(MemberNewsletter.create(memberId, subjectNewsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(memberId, contentNewsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(memberId, nameNewsletter.getId()));
        memberNewsletterRepository.save(MemberNewsletter.create(memberId, unmatchedNewsletter.getId()));

        NewsletterIssue subjectMatchedIssue = newsletterIssueRepository.save(
                createIssue(memberId, subjectNewsletter.getId(), 30L, "스프링 릴리즈 소식",
                        "다른 본문", "다른 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        NewsletterIssue contentMatchedIssue = newsletterIssueRepository.save(
                createIssue(memberId, contentNewsletter.getId(), 31L, "다른 제목",
                        "스프링 핵심 정리", "다른 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue newsletterNameMatchedIssue = newsletterIssueRepository.save(
                createIssue(memberId, nameNewsletter.getId(), 32L, "다른 제목",
                        "다른 본문", "다른 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(memberId, unmatchedNewsletter.getId(), 33L, "다른 제목",
                        "다른 본문", "다른 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findAllByMemberId(
                memberId,
                NewsletterIssueSearchCondition.withKeyword(keyword),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(
                        newsletterNameMatchedIssue.getId(),
                        contentMatchedIssue.getId(),
                        subjectMatchedIssue.getId()
                );
    }

    @Test
    @DisplayName("공개 피드 이슈를 카테고리로 필터링하고 수신시각 최신순으로 조회한다")
    void findPublicIssuesByMemberId_returns_public_issues_filtered_by_category_ordered_by_receivedAt_desc() {
        // given
        Long collectorMemberId = 1L;
        Long otherMemberId = 2L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter secondTechNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("두 번째 테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 40L, "오래된 공개 이슈",
                        "오래된 본문", "오래된 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        NewsletterIssue middleIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, secondTechNewsletter.getId(), 41L, "중간 공개 이슈",
                        "중간 본문", "중간 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue latestIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 42L, "최신 공개 이슈",
                        "최신 본문", "최신 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 43L, "다른 카테고리 이슈",
                        "다른 카테고리 본문", "다른 카테고리 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, techNewsletter.getId(), 44L, "다른 회원 이슈",
                        "다른 회원 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T04:00:00Z"))
        );

        NewsletterIssue deletedIssue = createIssue(collectorMemberId, techNewsletter.getId(), 45L, "삭제된 공개 이슈",
                "삭제된 본문", "삭제된 미리보기", Instant.parse("2050-05-12T05:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberId(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.TECH, null),
                PageRequest.of(0, 2)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(latestIssue.getId(), middleIssue.getId());

        NewsletterIssueItem latestIssueItem = result.getContent().get(0);
        assertThat(latestIssueItem.newsletterId()).isEqualTo(techNewsletter.getId());
        assertThat(latestIssueItem.newsletterName()).isEqualTo(techNewsletter.getName());
        assertThat(latestIssueItem.newsletterImageUrl()).isEqualTo(techNewsletter.getImageUrl());
        assertThat(latestIssueItem.newsletterCategory().code()).isEqualTo(NewsletterCategory.TECH.name());
        assertThat(latestIssueItem.newsletterCategory().label()).isEqualTo(NewsletterCategory.TECH.label());
        assertThat(latestIssueItem.subject()).isEqualTo("최신 공개 이슈");
        assertThat(latestIssueItem.previewText()).isEqualTo("최신 미리보기");
        assertThat(latestIssueItem.receivedAt()).isEqualTo(Instant.parse("2050-05-12T02:00:00Z"));
    }

    @Test
    @DisplayName("카테고리 조건이 없으면 공개 피드 이슈를 전체 카테고리에서 조회한다")
    void findPublicIssuesByMemberId_returns_public_issues_without_category_filter() {
        // given
        Long collectorMemberId = 1L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("전체 테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("전체 비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        NewsletterIssue techIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 50L, "전체 테크 이슈",
                        "전체 테크 본문", "전체 테크 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        NewsletterIssue bizIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 51L, "전체 비즈 이슈",
                        "전체 비즈 본문", "전체 비즈 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberId(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(null, null),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(bizIssue.getId(), techIssue.getId());
    }

    @Test
    @DisplayName("공개 피드 검색은 제목과 본문을 대상으로 하고 카테고리 조건과 함께 적용된다")
    void findPublicIssuesByMemberId_searches_subject_and_content_with_category_filter() {
        // given
        Long collectorMemberId = 1L;
        Long otherMemberId = 2L;
        String keyword = "redis";

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("검색 테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter keywordNameNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("redis 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("검색 비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        NewsletterIssue subjectMatchedIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 60L, "redis 운영 사례",
                        "다른 본문", "제목 매칭 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        NewsletterIssue contentMatchedIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 61L, "다른 제목",
                        "본문에서 redis 캐시 전략을 다룬다", "본문 매칭 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, keywordNameNewsletter.getId(), 62L, "다른 제목",
                        "다른 본문", "뉴스레터 이름만 매칭 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 63L, "redis 비즈 이슈",
                        "다른 본문", "다른 카테고리 미리보기", Instant.parse("2050-05-12T04:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, techNewsletter.getId(), 64L, "redis 다른 회원 이슈",
                        "다른 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T05:00:00Z"))
        );
        NewsletterIssue deletedIssue = createIssue(collectorMemberId, techNewsletter.getId(), 65L, "redis 삭제 이슈",
                "다른 본문", "삭제 이슈 미리보기", Instant.parse("2050-05-12T06:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberId(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.TECH, keyword),
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
    @DisplayName("공개 피드 검색어가 공백이면 키워드 조건 없이 조회한다")
    void findPublicIssuesByMemberId_ignores_blank_keyword() {
        // given
        Long collectorMemberId = 1L;

        Newsletter techNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("공백 검색 테크 뉴스레터", NewsletterCategory.TECH)
        );
        Newsletter bizNewsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("공백 검색 비즈 뉴스레터", NewsletterCategory.BIZ)
        );

        NewsletterIssue oldIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 70L, "오래된 공개 이슈",
                        "오래된 본문", "오래된 미리보기", Instant.parse("2050-05-12T00:00:00Z"))
        );
        NewsletterIssue latestIssue = newsletterIssueRepository.save(
                createIssue(collectorMemberId, techNewsletter.getId(), 71L, "최신 공개 이슈",
                        "최신 본문", "최신 미리보기", Instant.parse("2050-05-12T01:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(collectorMemberId, bizNewsletter.getId(), 72L, "다른 카테고리 이슈",
                        "다른 카테고리 본문", "다른 카테고리 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );

        entityManager.flush();
        entityManager.clear();

        // when
        Slice<NewsletterIssueItem> result = newsletterIssueRepository.findPublicIssuesByMemberId(
                collectorMemberId,
                new PublicNewsletterIssueSearchCondition(NewsletterCategory.TECH, "   "),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .extracting(NewsletterIssueItem::issueId)
                .containsExactly(latestIssue.getId(), oldIssue.getId());
    }

    @Test
    @DisplayName("회원의 삭제되지 않은 이슈 상세를 뉴스레터 정보와 함께 조회한다")
    void findDetailByMemberIdAndIssueId_returns_issue_detail_with_newsletter_info() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;

        Newsletter newsletter = newslettersRepository.save(
                NewsletterFixture.createNewsletter("상세 뉴스레터", NewsletterCategory.TECH)
        );

        NewsletterIssue targetIssue = createIssue(
                memberId,
                newsletter.getId(),
                10L,
                "상세 이슈",
                "상세 본문",
                "상세 미리보기",
                Instant.parse("2050-05-12T01:00:00Z")
        );
        targetIssue.markRead();
        newsletterIssueRepository.save(targetIssue);

        newsletterIssueRepository.save(
                createIssue(memberId, newsletter.getId(), 11L, "다른 이슈",
                        "다른 본문", "다른 미리보기", Instant.parse("2050-05-12T02:00:00Z"))
        );
        newsletterIssueRepository.save(
                createIssue(otherMemberId, newsletter.getId(), 12L, "다른 회원 이슈",
                        "다른 회원 본문", "다른 회원 미리보기", Instant.parse("2050-05-12T03:00:00Z"))
        );

        NewsletterIssue deletedIssue = createIssue(memberId, newsletter.getId(), 13L, "삭제된 이슈",
                "삭제된 본문", "삭제된 미리보기", Instant.parse("2050-05-12T04:00:00Z"));
        deletedIssue.deleteFromList();
        newsletterIssueRepository.save(deletedIssue);

        entityManager.flush();
        entityManager.clear();

        // when
        NewsletterIssueDetail detail = newsletterIssueRepository
                .findDetailByMemberIdAndIssueId(memberId, targetIssue.getId())
                .orElseThrow();

        // then
        assertThat(detail.issueId()).isEqualTo(targetIssue.getId());
        assertThat(detail.newsletterId()).isEqualTo(newsletter.getId());
        assertThat(detail.newsletterName()).isEqualTo(newsletter.getName());
        assertThat(detail.newsletterImageUrl()).isEqualTo(newsletter.getImageUrl());
        assertThat(detail.subject()).isEqualTo("상세 이슈");
        assertThat(detail.content()).isEqualTo("상세 본문");
        assertThat(detail.receivedAt()).isEqualTo(Instant.parse("2050-05-12T01:00:00Z"));
        assertThat(detail.read()).isTrue();
        assertThat(newsletterIssueRepository.findDetailByMemberIdAndIssueId(otherMemberId, targetIssue.getId())).isEmpty();
        assertThat(newsletterIssueRepository.findDetailByMemberIdAndIssueId(memberId, deletedIssue.getId())).isEmpty();
    }

    private NewsletterIssue createIssue(Long memberId, Long newsletterId, Long inboundEmailId,
                                        String subject, String content, String previewText, Instant receivedAt) {
        return NewsletterIssue.create(memberId, newsletterId, inboundEmailId,
                                      subject, content, previewText, receivedAt);
    }
}
