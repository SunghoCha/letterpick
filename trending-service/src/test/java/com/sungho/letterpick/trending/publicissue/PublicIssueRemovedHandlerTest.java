package com.sungho.letterpick.trending.publicissue;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.PublicIssueRemovedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import com.sungho.letterpick.trending.ranking.adapter.redis.RedisPublicIssueRankingStateWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicIssueRemovedHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-08T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicIssueCandidateRepository publicIssueCandidateRepository;

    @Mock
    private RedisPublicIssueRankingStateWriter rankingStateWriter;

    private PublicIssueRemovedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PublicIssueRemovedHandler(
                OBJECT_MAPPER,
                publicIssueCandidateRepository,
                rankingStateWriter,
                CLOCK
        );
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED eventType을 처리한다")
    void supports_public_issue_removed_event_type() {
        assertThat(handler.eventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_REMOVED.value());
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED payload를 REMOVED 상태 후보로 저장한다")
    void upsert_removed_candidate_status() {
        // given
        Instant collectedAt = Instant.parse("2050-06-05T00:59:00Z");
        EventEnvelope<JsonNode> envelope = envelope(new PublicIssueRemovedPayload(
                1L,
                collectedAt
        ));

        // when
        handler.handle(envelope);

        // then
        verify(publicIssueCandidateRepository).upsertRemoved(
                1L,
                CLOCK.instant(),
                CLOCK.instant()
        );
        verify(rankingStateWriter).markRemoved(1L, collectedAt);
    }

    @Test
    @DisplayName("payload 필수 필드가 누락되면 REMOVED 상태 후보를 저장하지 않는다")
    void reject_payload_with_missing_required_fields() {
        // given
        JsonNode invalidPayload = OBJECT_MAPPER.createObjectNode();
        EventEnvelope<JsonNode> envelope = envelope(invalidPayload);

        // when & then
        assertThatThrownBy(() -> handler.handle(envelope))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize PUBLIC_ISSUE_REMOVED payload");
        verifyNoInteractions(publicIssueCandidateRepository, rankingStateWriter);
    }

    private EventEnvelope<JsonNode> envelope(PublicIssueRemovedPayload payload) {
        return envelope(OBJECT_MAPPER.valueToTree(payload));
    }

    private EventEnvelope<JsonNode> envelope(JsonNode payload) {
        return new EventEnvelope<>(
                "event-1",
                TrendingEventType.PUBLIC_ISSUE_REMOVED.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );
    }
}
