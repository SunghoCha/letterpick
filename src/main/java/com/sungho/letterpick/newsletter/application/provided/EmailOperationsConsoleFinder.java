package com.sungho.letterpick.newsletter.application.provided;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface EmailOperationsConsoleFinder {

    InboundEmailStatusSummary findStatusSummary();

    Slice<InboundEmailActionRequiredItem> findActionRequiredItems(Pageable pageable);
}
