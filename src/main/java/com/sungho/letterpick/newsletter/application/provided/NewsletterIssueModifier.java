package com.sungho.letterpick.newsletter.application.provided;

import java.time.Instant;

public interface NewsletterIssueModifier {

    Instant delete(Long memberId, Long issueId);
}
