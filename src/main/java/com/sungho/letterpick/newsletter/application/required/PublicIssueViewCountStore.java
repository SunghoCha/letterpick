package com.sungho.letterpick.newsletter.application.required;

public interface PublicIssueViewCountStore {

    long incrementIfFirstView(Long issueId, String actorKey);
}
