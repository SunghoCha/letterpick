package com.sungho.letterpick.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    Optional<OutboxMessage> findByEventId(String eventId);

    List<OutboxMessage> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<OutboxMessageStatus> statuses,
            Instant nextAttemptAt,
            Pageable pageable
    );
}
