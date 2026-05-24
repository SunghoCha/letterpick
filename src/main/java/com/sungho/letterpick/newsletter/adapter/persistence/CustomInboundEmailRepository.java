package com.sungho.letterpick.newsletter.adapter.persistence;

import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;

import java.time.Instant;
import java.util.List;

public interface CustomInboundEmailRepository {

    List<InboundEmailStatusCount> countByStatus(Instant receivedFrom, Instant receivedTo);
}
