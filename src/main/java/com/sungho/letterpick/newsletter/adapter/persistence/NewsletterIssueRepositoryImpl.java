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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class NewsletterIssueRepositoryImpl implements CustomNewsletterIssueRepository {

    private final QNewsletterIssue newsletterIssue = QNewsletterIssue.newsletterIssue;
    private final QNewsletter newsletter = QNewsletter.newsletter;
    private final QMemberNewsletter memberNewsletter = QMemberNewsletter.memberNewsletter;
    private final JPAQueryFactory jpaQueryFactory;

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
                        categoryEq(condition.category())
                )
                .orderBy(newsletterIssue.receivedAt.desc(), newsletterIssue.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();
        List<NewsletterIssueItem> items = hasNext ? results.subList(0, pageable.getPageSize()) : results;

        return new SliceImpl<>(items, pageable, hasNext);
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
}
