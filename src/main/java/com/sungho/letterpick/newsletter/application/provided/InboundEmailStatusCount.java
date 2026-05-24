package com.sungho.letterpick.newsletter.application.provided;

import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;

public record InboundEmailStatusCount(
        InboundEmailStatus status,
        Long count
) {
}
