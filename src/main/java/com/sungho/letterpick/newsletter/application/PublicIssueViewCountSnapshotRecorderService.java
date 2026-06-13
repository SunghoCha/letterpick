package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.common.outbox.OutboxMessageRecordRequest;
import com.sungho.letterpick.common.outbox.OutboxMessageRecorder;
import com.sungho.letterpick.common.outbox.OutboxMessageType;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.newsletter.adapter.persistence.PublicIssueViewCountRepository;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountSnapshotRecorder;
import com.sungho.letterpick.newsletter.domain.PublicIssueViewCount;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PublicIssueViewCountSnapshotRecorderService implements PublicIssueViewCountSnapshotRecorder {

    private final PublicIssueViewCountRepository publicIssueViewCountRepository;
    private final OutboxMessageRecorder outboxMessageRecorder;
    private final Clock clock;

    @Override
    @Transactional
    @WithSpan("public_issue_view.snapshot_record")
    public void recordSnapshot(@SpanAttribute("issue.id") Long issueId,
                               @SpanAttribute("view.count") long viewCount) {
        Instant occurredAt = clock.instant();
        publicIssueViewCountRepository.upsertSnapshot(issueId, viewCount, occurredAt);
        PublicIssueViewCount snapshot = publicIssueViewCountRepository.findById(issueId).orElseThrow();
        if (snapshot.getViewCount() != viewCount) {
            return;
        }
        outboxMessageRecorder.record(new OutboxMessageRecordRequest(
                UUID.randomUUID().toString(),
                OutboxMessageType.ISSUE_VIEW_COUNT_UPDATED,
                String.valueOf(issueId),
                new IssueViewCountUpdatedPayload(issueId, viewCount),
                occurredAt
        ));
    }
}
