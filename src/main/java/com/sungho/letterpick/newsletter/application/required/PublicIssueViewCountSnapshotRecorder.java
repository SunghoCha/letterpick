package com.sungho.letterpick.newsletter.application.required;

public interface PublicIssueViewCountSnapshotRecorder {

    void recordSnapshot(Long issueId, long viewCount);
}
