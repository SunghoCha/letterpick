package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailActionRequiredItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

public interface CustomInboundEmailRepository {

    List<InboundEmailStatusCount> countByStatus(Instant receivedFrom, Instant receivedTo);

    Slice<InboundEmailActionRequiredItem> findActionRequired(Instant receivedFrom, Instant receivedTo, Pageable pageable);
}
