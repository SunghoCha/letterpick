package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

public interface CustomInboundEmailRepository {

    List<InboundEmailStatusCount> countByStatus(Instant receivedFrom, Instant receivedTo);

    Slice<InboundEmailAdminItem> findActionRequired(Instant receivedFrom, Instant receivedTo, Pageable pageable);

    Slice<InboundEmailAdminItem> findStaleReceived(Instant receivedBefore, Pageable pageable);
}
