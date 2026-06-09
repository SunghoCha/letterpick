package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.NewsletterIssueRepository;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecordRequest;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecorder;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountSnapshotRecorder;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicNewsletterIssueViewCountRecordService implements PublicNewsletterIssueViewCountRecorder {

    private final PublicFeedCollectorAccount publicFeedCollectorAccount;
    private final NewsletterIssueRepository newsletterIssueRepository;
    private final PublicIssueViewCountStore publicIssueViewCountStore;
    private final PublicIssueViewCountSnapshotRecorder publicIssueViewCountSnapshotRecorder;
    private final PublicIssueViewCountProperties properties;

    @Override
    public void record(PublicNewsletterIssueViewCountRecordRequest request) {
        Long collectorMemberId = publicFeedCollectorAccount.collectorMemberId();
        if (!newsletterIssueRepository.existsByIdAndMemberIdAndDeletedFalse(request.issueId(), collectorMemberId)) {
            return;
        }

        long incrementedViewCount = publicIssueViewCountStore.incrementIfFirstView(request.issueId(), request.actorKey());
        if (incrementedViewCount == 0) {
            return;
        }
        if (incrementedViewCount % properties.snapshotInterval() == 0) {
            publicIssueViewCountSnapshotRecorder.recordSnapshot(request.issueId(), incrementedViewCount);
        }
    }
}
