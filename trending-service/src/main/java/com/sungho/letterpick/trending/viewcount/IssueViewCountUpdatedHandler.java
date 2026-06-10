package com.sungho.letterpick.trending.viewcount;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingEventHandler;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

@Component
public class IssueViewCountUpdatedHandler implements TrendingEventHandler {

    private final ObjectMapper objectMapper;
    private final PublicIssueViewCountSnapshotRepository repository;
    private final Clock clock;

    public IssueViewCountUpdatedHandler(ObjectMapper objectMapper,
                                        PublicIssueViewCountSnapshotRepository repository,
                                        Clock clock) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
        this.repository = requireNonNull(repository, "repository must not be null");
        this.clock = requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String eventType() {
        return TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value();
    }

    @Override
    public void handle(EventEnvelope<JsonNode> envelope) {
        IssueViewCountUpdatedPayload payload = readPayload(envelope.payload());
        repository.upsertSnapshot(
                payload.issueId(),
                payload.viewCount(),
                envelope.occurredAt(),
                clock.instant()
        );
    }

    private IssueViewCountUpdatedPayload readPayload(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, IssueViewCountUpdatedPayload.class);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to deserialize ISSUE_VIEW_COUNT_UPDATED payload", e);
        }
    }
}
