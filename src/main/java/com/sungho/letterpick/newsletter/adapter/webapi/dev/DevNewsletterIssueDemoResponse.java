package com.sungho.letterpick.newsletter.adapter.webapi.dev;

import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoResult;

public record DevNewsletterIssueDemoResponse(
        int createdIssueCount,
        int skippedIssueCount
) {

    public static DevNewsletterIssueDemoResponse from(DevNewsletterIssueDemoResult result) {
        return new DevNewsletterIssueDemoResponse(
                result.createdIssueCount(),
                result.skippedIssueCount()
        );
    }
}
