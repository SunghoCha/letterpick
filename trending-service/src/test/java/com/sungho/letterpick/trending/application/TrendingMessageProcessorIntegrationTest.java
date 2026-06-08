package com.sungho.letterpick.trending.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import com.sungho.letterpick.trending.inbox.InboxEvent;
import com.sungho.letterpick.trending.inbox.InboxEventRepository;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TrendingServiceTestConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class TrendingMessageProcessorIntegrationTest {

    private static final String QUEUE_NAME = "letterpick-test-trending-lifecycle-events";

    @Autowired
    private TrendingMessageProcessor processor;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private PublicIssueCandidateRepository publicIssueCandidateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        inboxEventRepository.deleteAll();
        publicIssueCandidateRepository.deleteAll();
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_AVAILABLE 메시지를 처리하면 inbox와 공개 이슈 후보를 저장한다")
    void process_public_issue_available_message() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-1", 1L, 2L);

        // when
        processor.process(message, QUEUE_NAME);

        // then
        InboxEvent inboxEvent = inboxEventRepository.findByEventId("event-1").orElseThrow();
        assertThat(inboxEvent.getEventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value());
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEvent.getProcessedAt()).isNotNull();
        assertThat(inboxEvent.getQueueName()).isEqualTo(QUEUE_NAME);
        JsonNode storedPayload = objectMapper.readTree(inboxEvent.getPayload());
        assertThat(storedPayload.path("issueId").asLong()).isEqualTo(1L);
        assertThat(storedPayload.path("newsletterId").asLong()).isEqualTo(2L);
        assertThat(storedPayload.path("category").asText()).isEqualTo("TECH");

        PublicIssueCandidate candidate = publicIssueCandidateRepository.findByIssueId(1L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isEqualTo(2L);
        assertThat(candidate.getCategory()).isEqualTo("TECH");
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.AVAILABLE);
        assertThat(candidate.getPublicFeedCollectedAt()).isEqualTo(Instant.parse("2050-06-05T00:59:00Z"));
    }

    @Test
    @DisplayName("이미 처리된 eventId가 다시 오면 공개 이슈 후보를 중복 저장하지 않는다")
    void skip_already_processed_event_id() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-2", 10L, 20L);
        processor.process(message, QUEUE_NAME);

        // when
        processor.process(message, QUEUE_NAME);

        // then
        assertThat(inboxEventRepository.findByEventId("event-2").orElseThrow().getStatus())
                .isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(publicIssueCandidateRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 eventId가 같은 issueId를 후보 등록해도 공개 이슈 후보를 중복 저장하지 않는다")
    void process_duplicate_issue_id_as_noop() throws Exception {
        // given
        String firstMessage = publicIssueAvailableMessage("event-3", 100L, 200L);
        String secondMessage = publicIssueAvailableMessage("event-4", 100L, 300L);

        // when
        processor.process(firstMessage, QUEUE_NAME);
        processor.process(secondMessage, QUEUE_NAME);

        // then
        assertThat(inboxEventRepository.findByEventId("event-3").orElseThrow().getStatus())
                .isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEventRepository.findByEventId("event-4").orElseThrow().getStatus())
                .isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(publicIssueCandidateRepository.count()).isEqualTo(1);

        PublicIssueCandidate candidate = publicIssueCandidateRepository.findByIssueId(100L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("지원하지 않는 eventType은 inbox를 FAILED로 기록하고 예외를 유지한다")
    void mark_inbox_failed_when_event_type_is_unsupported() throws Exception {
        // given
        String message = message("event-unsupported", "UNKNOWN_EVENT", Map.of("value", "x"));

        // when & then
        assertThatThrownBy(() -> processor.process(message, QUEUE_NAME))
                .isInstanceOf(TrendingMessageProcessingException.class)
                .hasMessageContaining("unsupported trending event type");

        InboxEvent inboxEvent = inboxEventRepository.findByEventId("event-unsupported").orElseThrow();
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.FAILED);
        assertThat(inboxEvent.getProcessedAt()).isNull();
        assertThat(inboxEvent.getLastError()).contains("unsupported trending event type");
        assertThat(publicIssueCandidateRepository.count()).isZero();
    }

    private String publicIssueAvailableMessage(String eventId, Long issueId, Long newsletterId) throws Exception {
        return message(
                eventId,
                TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value(),
                new PublicIssueAvailablePayload(
                        issueId,
                        newsletterId,
                        "TECH",
                        Instant.parse("2050-06-05T00:59:00Z")
                )
        );
    }

    private String message(String eventId, String eventType, Object payload) throws Exception {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                1,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );
        return objectMapper.writeValueAsString(envelope);
    }
}
