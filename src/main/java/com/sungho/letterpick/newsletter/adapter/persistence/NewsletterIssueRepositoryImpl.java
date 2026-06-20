package com.sungho.letterpick.newsletter.adapter.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.MemberNewsletterStatus;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.QMemberNewsletter;
import com.sungho.letterpick.newsletter.domain.QNewsletter;
import com.sungho.letterpick.newsletter.domain.QNewsletterIssue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class NewsletterIssueRepositoryImpl implements CustomNewsletterIssueRepository {

    private final QNewsletterIssue newsletterIssue = QNewsletterIssue.newsletterIssue;
    private final QNewsletter newsletter = QNewsletter.newsletter;
    private final QMemberNewsletter memberNewsletter = QMemberNewsletter.memberNewsletter;
    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    @Override
    public Slice<NewsletterIssueItem> findAllByMemberId(Long memberId, NewsletterIssueSearchCondition condition, Pageable pageable) {
        requireNonNull(memberId);
        requireNonNull(condition);
        requireNonNull(pageable);

        List<NewsletterIssueItem> results = jpaQueryFactory
                .select(Projections.constructor(
                        NewsletterIssueItem.class,
                        newsletterIssue.id,
                        newsletterIssue.newsletterId,
                        newsletter.name,
                        newsletter.imageUrl,
                        newsletter.category,
                        newsletterIssue.subject,
                        newsletterIssue.previewText,
                        newsletterIssue.receivedAt,
                        newsletterIssue.read
                ))
                .from(newsletterIssue)
                .join(newsletter).on(newsletter.id.eq(newsletterIssue.newsletterId))
                .join(memberNewsletter)
                .on(
                        memberNewsletter.memberId.eq(newsletterIssue.memberId),
                        memberNewsletter.newsletterId.eq(newsletterIssue.newsletterId)
                )
                .where(
                        newsletterIssue.memberId.eq(memberId),
                        newsletterIssue.deleted.isFalse(),
                        receivedAtGoe(condition.receivedFrom()),
                        receivedAtLt(condition.receivedTo()),
                        keywordContains(condition.keyword()),
                        memberNewsletter.status.eq(MemberNewsletterStatus.ACTIVE)
                )
                .orderBy(newsletterIssue.receivedAt.desc(), newsletterIssue.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();
        List<NewsletterIssueItem> content = hasNext ? results.subList(0, pageable.getPageSize()) : results;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<NewsletterIssueItem> findPublicIssuesByMemberId(Long memberId,
                                                                 PublicNewsletterIssueSearchCondition condition,
                                                                 Pageable pageable) {
        requireNonNull(memberId);
        requireNonNull(condition);
        requireNonNull(pageable);

        List<NewsletterIssueItem> results = jpaQueryFactory
                .select(Projections.constructor(
                        NewsletterIssueItem.class,
                        newsletterIssue.id,
                        newsletterIssue.newsletterId,
                        newsletter.name,
                        newsletter.imageUrl,
                        newsletter.category,
                        newsletterIssue.subject,
                        newsletterIssue.previewText,
                        newsletterIssue.receivedAt,
                        newsletterIssue.read
                ))
                .from(newsletterIssue)
                .join(newsletter)
                .on(newsletter.id.eq(newsletterIssue.newsletterId))
                .where(
                        newsletterIssue.memberId.eq(memberId),
                        newsletterIssue.deleted.isFalse(),
                        categoryEq(condition.category()),
                        publicKeywordContains(condition.keyword())
                )
                .orderBy(newsletterIssue.receivedAt.desc(), newsletterIssue.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();
        List<NewsletterIssueItem> items = hasNext ? results.subList(0, pageable.getPageSize()) : results;

        return new SliceImpl<>(items, pageable, hasNext);
    }

    @Override
    public Slice<NewsletterIssueItem> findPublicIssuesByMemberIdWithFullText(Long memberId,
                                                                             PublicNewsletterIssueSearchCondition condition,
                                                                             Pageable pageable) {
        requireNonNull(memberId);
        requireNonNull(condition);
        requireNonNull(pageable);

        String booleanQuery = toAllTermsFullTextBooleanQuery(condition.keyword());
        Query query = createPublicFullTextQuery(memberId, condition.category(), booleanQuery, pageable);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<NewsletterIssueItem> results = rows.stream()
                .map(this::toNewsletterIssueItem)
                .toList();

        boolean hasNext = results.size() > pageable.getPageSize();
        List<NewsletterIssueItem> items = hasNext ? results.subList(0, pageable.getPageSize()) : results;

        return new SliceImpl<>(items, pageable, hasNext);
    }

    @Override
    public List<NewsletterIssueItem> findPublicIssuesByMemberIdAndIssueIds(Long memberId, List<Long> issueIds) {
        requireNonNull(memberId);
        requireNonNull(issueIds);

        if (issueIds.isEmpty()) {
            return List.of();
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        NewsletterIssueItem.class,
                        newsletterIssue.id,
                        newsletterIssue.newsletterId,
                        newsletter.name,
                        newsletter.imageUrl,
                        newsletter.category,
                        newsletterIssue.subject,
                        newsletterIssue.previewText,
                        newsletterIssue.receivedAt,
                        newsletterIssue.read
                ))
                .from(newsletterIssue)
                .join(newsletter)
                .on(newsletter.id.eq(newsletterIssue.newsletterId))
                .where(
                        newsletterIssue.memberId.eq(memberId),
                        newsletterIssue.id.in(issueIds),
                        newsletterIssue.deleted.isFalse()
                )
                .fetch();
    }

    private BooleanExpression categoryEq(NewsletterCategory category) {
        return category == null ? null : newsletter.category.eq(category);
    }

    @Override
    public Optional<NewsletterIssueDetail> findDetailByMemberIdAndIssueId(Long memberId, Long issueId) {
        requireNonNull(memberId);
        requireNonNull(issueId);

        NewsletterIssueDetail detail = jpaQueryFactory
                .select(Projections.constructor(
                        NewsletterIssueDetail.class,
                        newsletterIssue.id,
                        newsletterIssue.newsletterId,
                        newsletter.name,
                        newsletter.imageUrl,
                        newsletterIssue.subject,
                        newsletterIssue.content,
                        newsletterIssue.receivedAt,
                        newsletterIssue.read
                ))
                .from(newsletterIssue)
                .join(newsletter)
                .on(newsletter.id.eq(newsletterIssue.newsletterId))
                .where(
                        newsletterIssue.memberId.eq(memberId),
                        newsletterIssue.id.eq(issueId),
                        newsletterIssue.deleted.isFalse()
                )
                .fetchOne();

        return Optional.ofNullable(detail);
    }

    private BooleanExpression receivedAtLt(Instant receivedTo) {
        return receivedTo == null ? null : newsletterIssue.receivedAt.lt(receivedTo);
    }

    private BooleanExpression receivedAtGoe(Instant receivedFrom) {
        return receivedFrom == null ? null : newsletterIssue.receivedAt.goe(receivedFrom);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        return newsletterIssue.subject.contains(trimmedKeyword)
                .or(newsletterIssue.content.contains(trimmedKeyword))
                .or(newsletter.name.contains(trimmedKeyword));
    }

    private BooleanExpression publicKeywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmedKeyword = keyword.trim();

        return newsletterIssue.subject.contains(trimmedKeyword)
                .or(newsletterIssue.content.contains(trimmedKeyword));
    }

    private Query createPublicFullTextQuery(Long memberId,
                                            NewsletterCategory category,
                                            String booleanQuery,
                                            Pageable pageable) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    ni.id,
                    ni.newsletter_id,
                    n.name,
                    n.image_url,
                    n.category,
                    ni.subject,
                    ni.preview_text,
                    ni.received_at,
                    ni.read_status
                FROM newsletter_issue ni
                JOIN newsletter n
                    ON n.id = ni.newsletter_id
                WHERE ni.member_id = :memberId
                    AND ni.deleted = false
                """);

        if (category != null) {
            sql.append("""
                     AND n.category = :category
                    """);
        }
        if (booleanQuery != null) {
            sql.append("""
                     AND MATCH(ni.subject, ni.content) AGAINST (:keyword IN BOOLEAN MODE)
                    """);
        }

        if (booleanQuery != null) {
            sql.append("""
                    ORDER BY MATCH(ni.subject, ni.content) AGAINST (:keyword IN BOOLEAN MODE) DESC
                    """);
        } else {
            sql.append("""
                    ORDER BY ni.received_at DESC, ni.id DESC
                    """);
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("memberId", memberId);
        if (category != null) {
            query.setParameter("category", category.name());
        }
        if (booleanQuery != null) {
            query.setParameter("keyword", booleanQuery);
        }
        query.setMaxResults(pageable.getPageSize() + 1);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));

        return query;
    }

    private NewsletterIssueItem toNewsletterIssueItem(Object[] row) {
        return new NewsletterIssueItem(
                toLong(row[0]),
                toLong(row[1]),
                (String) row[2],
                (String) row[3],
                NewsletterCategory.valueOf((String) row[4]),
                (String) row[5],
                (String) row[6],
                toInstant(row[7]),
                toBoolean(row[8])
        );
    }

    private String toSanitizedFullTextQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String query = keyword.trim()
                .replaceAll("[+\\-<>()~*@\"]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (query.isBlank()) {
            return null;
        }

        return query;
    }

    private String toAllTermsFullTextBooleanQuery(String keyword) {
        String query = toSanitizedFullTextQuery(keyword);
        if (query == null) {
            return null;
        }

        return toAllTermsBooleanQuery(query);
    }

    private String toAllTermsBooleanQuery(String query) {
        return Arrays.stream(query.split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(token -> "+" + token)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
    }

    private Long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }

        throw new IllegalArgumentException("Unsupported instant value type: " + value.getClass());
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }

        throw new IllegalArgumentException("Unsupported boolean value type: " + value.getClass());
    }
}
