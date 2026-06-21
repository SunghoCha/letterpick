package com.sungho.letterpick.newsletter.application.exception;

import com.sungho.letterpick.common.exception.BusinessException;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterErrorCode;

public class PublicIssueRankingReadException extends BusinessException {

    public PublicIssueRankingReadException() {
        super(NewsletterErrorCode.PUBLIC_ISSUE_RANKING_UNAVAILABLE);
    }

    public PublicIssueRankingReadException(Throwable cause) {
        super(NewsletterErrorCode.PUBLIC_ISSUE_RANKING_UNAVAILABLE, cause);
    }
}
