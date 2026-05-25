package com.sungho.letterpick.newsletter.adapter.persistence;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.INVALID_RECIPIENT_ADDRESS;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.NEWSLETTER_NOT_FOUND;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECIPIENT_NOT_FOUND;
import static java.util.Objects.requireNonNull;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailActionRequiredItem;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;
import com.sungho.letterpick.newsletter.domain.QInboundEmail;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class InboundEmailRepositoryImpl implements CustomInboundEmailRepository {

    private static final List<InboundEmailStatus> ACTION_REQUIRED_STATUSES = List.of(
            NEWSLETTER_NOT_FOUND,
            RECIPIENT_NOT_FOUND,
            INVALID_RECIPIENT_ADDRESS
    );

    private final QInboundEmail inboundEmail = QInboundEmail.inboundEmail;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<InboundEmailStatusCount> countByStatus(Instant receivedFrom, Instant receivedTo) {
        requireNonNull(receivedFrom, "receivedFrom must not be null");
        requireNonNull(receivedTo, "receivedTo must not be null");
        if (!receivedFrom.isBefore(receivedTo)) {
            throw new IllegalArgumentException("receivedFrom must be before receivedTo");
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        InboundEmailStatusCount.class,
                        inboundEmail.status,
                        inboundEmail.id.count()
                ))
                .from(inboundEmail)
                .where(
                        inboundEmail.receivedAt.goe(receivedFrom),
                        inboundEmail.receivedAt.lt(receivedTo)
                )
                .groupBy(inboundEmail.status)
                .fetch();
    }

    @Override
    public Slice<InboundEmailActionRequiredItem> findActionRequired(Instant receivedFrom,
                                                                    Instant receivedTo,
                                                                    Pageable pageable) {
        requireNonNull(receivedFrom, "receivedFrom must not be null");
        requireNonNull(receivedTo, "receivedTo must not be null");
        requireNonNull(pageable, "pageable must not be null");
        if (!receivedFrom.isBefore(receivedTo)) {
            throw new IllegalArgumentException("receivedFrom must be before receivedTo");
        }

        List<InboundEmailActionRequiredItem> results = jpaQueryFactory
                .select(Projections.constructor(
                        InboundEmailActionRequiredItem.class,
                        inboundEmail.id,
                        inboundEmail.receivedAt,
                        inboundEmail.status,
                        inboundEmail.senderEmail,
                        inboundEmail.recipientAddress,
                        inboundEmail.subject,
                        inboundEmail.memberId,
                        inboundEmail.newsletterId,
                        inboundEmail.messageKey,
                        inboundEmail.rawReference
                ))
                .from(inboundEmail)
                .where(
                        inboundEmail.receivedAt.goe(receivedFrom),
                        inboundEmail.receivedAt.lt(receivedTo),
                        inboundEmail.status.in(ACTION_REQUIRED_STATUSES)
                )
                .orderBy(inboundEmail.receivedAt.desc(), inboundEmail.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();
        List<InboundEmailActionRequiredItem> content = hasNext ? results.subList(0, pageable.getPageSize()) : results;

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
