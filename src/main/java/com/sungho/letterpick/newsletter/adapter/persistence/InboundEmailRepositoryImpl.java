package com.sungho.letterpick.newsletter.adapter.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.domain.QInboundEmail;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class InboundEmailRepositoryImpl implements CustomInboundEmailRepository {

    private final QInboundEmail inboundEmail = QInboundEmail.inboundEmail;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<InboundEmailStatusCount> countByStatus(Instant receivedFrom, Instant receivedTo) {
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
