package com.sungho.letterpick.newsletter.application.provided;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface EmailOperationsConsoleFinder {

    InboundEmailStatusSummary findStatusSummary(EmailOperationsSearchCondition searchCondition);

    Slice<InboundEmailAdminItem> findActionRequiredItems(EmailOperationsSearchCondition searchCondition,
                                                         Pageable pageable);

    Slice<InboundEmailAdminItem> findStaleReceivedItems(Pageable pageable);

    EmailOperationsQueueStatus findQueueStatus();
}
