package com.sungho.letterpick.trending.viewcount;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueViewCountStateUpdater;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingScoreUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueViewCountUpdatedHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private static final Instant OCCURRED_AT = Instant.parse("2050-06-10T01:00:00Z");

    @Mock
    private RedisPublicIssueViewCountStateUpdater viewCountStateUpdater;

    @Mock
    private PublicIssueRankingScoreUpdater rankingScoreUpdater;

    private IssueViewCountUpdatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueViewCountUpdatedHandler(
                OBJECT_MAPPER,
                viewCountStateUpdater,
                rankingScoreUpdater
        );
    }

    @Test
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED eventType을 처리한다")
    void supports_issue_view_count_updated_event_type() {
        assertThat(handler.eventType()).isEqualTo(TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value());
    }

    @Test
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED payload로 ranking score를 갱신한다")
    void update_ranking_score() {
        // given
        EventEnvelope<JsonNode> envelope = envelope(new IssueViewCountUpdatedPayload(1L, 150L), OCCURRED_AT);
        when(viewCountStateUpdater.acceptIfAvailableAndNotStale(1L, 150L))
                .thenReturn(true);

        // when
        handler.handle(envelope);

        // then
        verify(viewCountStateUpdater).acceptIfAvailableAndNotStale(1L, 150L);
        verify(rankingScoreUpdater).refresh(1L, OCCURRED_AT);
        verifyNoMoreInteractions(rankingScoreUpdater);
    }

    @Test
    @DisplayName("늦게 도착한 조회수 이벤트이면 ranking을 갱신하지 않는다")
    void skip_ranking_when_view_count_state_is_stale() {
        // given
        EventEnvelope<JsonNode> envelope = envelope(new IssueViewCountUpdatedPayload(1L, 150L), OCCURRED_AT);
        when(viewCountStateUpdater.acceptIfAvailableAndNotStale(1L, 150L))
                .thenReturn(false);

        // when
        handler.handle(envelope);

        // then
        verify(viewCountStateUpdater).acceptIfAvailableAndNotStale(1L, 150L);
        verifyNoInteractions(rankingScoreUpdater);
    }

    @Test
    @DisplayName("payload 필수 필드가 누락되면 ranking을 갱신하지 않는다")
    void reject_payload_with_missing_required_fields() {
        // given
        JsonNode invalidPayload = OBJECT_MAPPER.createObjectNode()
                .put("issueId", 1L);
        EventEnvelope<JsonNode> envelope = envelope(
                invalidPayload,
                Instant.parse("2050-06-10T00:59:00Z")
        );

        // when & then
        assertThatThrownBy(() -> handler.handle(envelope))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize ISSUE_VIEW_COUNT_UPDATED payload");
        verifyNoInteractions(viewCountStateUpdater, rankingScoreUpdater);
    }

    private EventEnvelope<JsonNode> envelope(IssueViewCountUpdatedPayload payload, Instant occurredAt) {
        return envelope(OBJECT_MAPPER.valueToTree(payload), occurredAt);
    }

    private EventEnvelope<JsonNode> envelope(JsonNode payload, Instant occurredAt) {
        return new EventEnvelope<>(
                "event-1",
                TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
                1,
                "letterpick",
                occurredAt,
                "trace-1",
                payload
        );
    }
}
