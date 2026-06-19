package com.sungho.letterpick.trending.viewcount;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueViewCountStateUpdater;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingScoreUpdater;
import com.sungho.letterpick.trending.score.application.TrendingScoreEventHandler;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.util.Objects.requireNonNull;

@Component
public class IssueViewCountUpdatedHandler implements TrendingScoreEventHandler {

    private final ObjectMapper objectMapper;
    private final RedisPublicIssueViewCountStateUpdater viewCountStateUpdater;
    private final PublicIssueRankingScoreUpdater rankingScoreUpdater;

    public IssueViewCountUpdatedHandler(ObjectMapper objectMapper,
                                        RedisPublicIssueViewCountStateUpdater viewCountStateUpdater,
                                        PublicIssueRankingScoreUpdater rankingScoreUpdater) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
        this.viewCountStateUpdater = requireNonNull(viewCountStateUpdater, "viewCountStateUpdater must not be null");
        this.rankingScoreUpdater = requireNonNull(rankingScoreUpdater, "rankingScoreUpdater must not be null");
    }

    @Override
    public String eventType() {
        return TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value();
    }

    @Override
    @WithSpan("trending.issue_view_count.handle")
    public void handle(EventEnvelope<JsonNode> envelope) {
        IssueViewCountUpdatedPayload payload = readPayload(envelope.payload());
        if (viewCountStateUpdater.acceptIfAvailableAndNotStale(payload.issueId(), payload.viewCount())) {
            rankingScoreUpdater.refresh(payload.issueId(), envelope.occurredAt());
        }
    }

    private IssueViewCountUpdatedPayload readPayload(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, IssueViewCountUpdatedPayload.class);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to deserialize ISSUE_VIEW_COUNT_UPDATED payload", e);
        }
    }
}
