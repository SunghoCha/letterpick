package com.sungho.letterpick.trending.publicissue;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.PublicIssueRemovedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingEventHandler;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateWriter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static java.util.Objects.requireNonNull;

@Component
public class PublicIssueRemovedHandler implements TrendingEventHandler {

    private final ObjectMapper objectMapper;
    private final PublicIssueCandidateRepository publicIssueCandidateRepository;
    private final RedisPublicIssueRankingStateWriter rankingStateWriter;
    private final Clock clock;

    public PublicIssueRemovedHandler(ObjectMapper objectMapper,
                                     PublicIssueCandidateRepository publicIssueCandidateRepository,
                                     RedisPublicIssueRankingStateWriter rankingStateWriter,
                                     Clock clock) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
        this.publicIssueCandidateRepository = requireNonNull(publicIssueCandidateRepository,
                "publicIssueCandidateRepository must not be null");
        this.rankingStateWriter = requireNonNull(rankingStateWriter, "rankingStateWriter must not be null");
        this.clock = requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String eventType() {
        return TrendingEventType.PUBLIC_ISSUE_REMOVED.value();
    }

    @Override
    public void handle(EventEnvelope<JsonNode> envelope) {
        PublicIssueRemovedPayload payload = readPayload(envelope.payload());
        var now = clock.instant();
        publicIssueCandidateRepository.upsertRemoved(
                payload.issueId(),
                now,
                now
        );
        rankingStateWriter.markRemoved(payload.issueId(), payload.publicFeedCollectedAt());
    }

    private PublicIssueRemovedPayload readPayload(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, PublicIssueRemovedPayload.class);
        } catch (JacksonException e) {
            throw new TrendingMessageProcessingException("failed to deserialize PUBLIC_ISSUE_REMOVED payload", e);
        }
    }
}
