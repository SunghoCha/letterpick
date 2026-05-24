package com.sungho.letterpick.newsletter.adapter.persistence;

import static java.util.Objects.requireNonNull;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.domain.QInboundEmail;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InboundEmailRepositoryImpl implements CustomInboundEmailRepository {

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
}
