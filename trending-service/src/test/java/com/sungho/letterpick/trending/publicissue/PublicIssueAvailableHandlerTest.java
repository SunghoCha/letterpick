package com.sungho.letterpick.trending.publicissue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.application.TrendingMessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.sungho.letterpick.trending.support.TrendingTestObjectMapper.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicIssueAvailableHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-08T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PublicIssueCandidateRepository publicIssueCandidateRepository;

    private PublicIssueAvailableHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PublicIssueAvailableHandler(
                OBJECT_MAPPER,
                publicIssueCandidateRepository,
                CLOCK
        );
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_AVAILABLE eventType을 처리한다")
    void supports_public_issue_available_event_type() {
        assertThat(handler.eventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value());
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_AVAILABLE payload를 공개 이슈 후보로 저장한다")
    void insert_public_issue_candidate() {
        // given
        Instant collectedAt = Instant.parse("2050-06-05T00:59:00Z");
        EventEnvelope<JsonNode> envelope = envelope(new PublicIssueAvailablePayload(
                1L,
                2L,
                "TECH",
                collectedAt
        ));

        // when
        handler.handle(envelope);

        // then
        verify(publicIssueCandidateRepository).insertAvailableIfAbsent(
                1L,
                2L,
                "TECH",
                PublicIssueCandidateStatus.AVAILABLE.name(),
                collectedAt,
                CLOCK.instant(),
                CLOCK.instant()
        );
    }

    @Test
    @DisplayName("payload 필수 필드가 누락되면 공개 이슈 후보를 저장하지 않는다")
    void reject_payload_with_missing_required_fields() {
        // given
        JsonNode invalidPayload = OBJECT_MAPPER.createObjectNode()
                .put("issueId", 1L);
        EventEnvelope<JsonNode> envelope = envelope(invalidPayload);

        // when & then
        assertThatThrownBy(() -> handler.handle(envelope))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("failed to deserialize PUBLIC_ISSUE_AVAILABLE payload");
        verifyNoInteractions(publicIssueCandidateRepository);
    }

    private EventEnvelope<JsonNode> envelope(PublicIssueAvailablePayload payload) {
        return envelope(OBJECT_MAPPER.valueToTree(payload));
    }

    private EventEnvelope<JsonNode> envelope(JsonNode payload) {
        return new EventEnvelope<>(
                "event-1",
                TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );
    }
}
