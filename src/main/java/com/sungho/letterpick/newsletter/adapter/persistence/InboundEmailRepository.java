package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.domain.InboundEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboundEmailRepository extends JpaRepository<InboundEmail, Long>, CustomInboundEmailRepository {

    Optional<InboundEmail> findByMessageKey(String messageKey);

    boolean existsByMessageKey(String messageKey);
}
