package com.sungho.letterpick.trending.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.event.trending.IssueViewCountUpdatedPayload;
import com.sungho.letterpick.event.trending.PublicIssueAvailablePayload;
import com.sungho.letterpick.event.trending.PublicIssueRemovedPayload;
import com.sungho.letterpick.event.trending.TrendingEventType;
import com.sungho.letterpick.trending.TrendingServiceTestConfiguration;
import com.sungho.letterpick.trending.inbox.InboxEvent;
import com.sungho.letterpick.trending.inbox.InboxEventRepository;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidate;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateRepository;
import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.adapter.persistence.PublicIssueRankingSummaryRepository;
import com.sungho.letterpick.trending.score.application.TrendingScoreMessageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TrendingServiceTestConfiguration.class)
@SpringBootTest(properties = {
        "letterpick.trending.ranking.summary.writer=redis"
})
@ActiveProfiles("test")
class TrendingMessageProcessorIntegrationTest {

    private static final String LIFECYCLE_QUEUE_NAME = "letterpick-test-trending-lifecycle-events";

    @Autowired
    private TrendingMessageProcessor processor;

    @Autowired
    private TrendingScoreMessageProcessor scoreProcessor;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private PublicIssueCandidateRepository publicIssueCandidateRepository;

    @Autowired
    private PublicIssueRankingSummaryRepository publicIssueRankingSummaryRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        publicIssueRankingSummaryRepository.deleteAll();
        inboxEventRepository.deleteAll();
        publicIssueCandidateRepository.deleteAll();
        redisTemplate.getRequiredConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_AVAILABLE 메시지를 처리하면 inbox와 공개 이슈 후보를 저장한다")
    void process_public_issue_available_message() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-1", 1L, 2L);

        // when
        processor.process(message, LIFECYCLE_QUEUE_NAME);

        // then
        InboxEvent inboxEvent = inboxEventRepository.findByEventId("event-1").orElseThrow();
        assertThat(inboxEvent.getEventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_AVAILABLE.value());
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEvent.getProcessedAt()).isNotNull();
        assertThat(inboxEvent.getQueueName()).isEqualTo(LIFECYCLE_QUEUE_NAME);
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
    @DisplayName("ISSUE_VIEW_COUNT_UPDATED 메시지는 inbox 없이 처리하고 공개 상태가 없으면 ranking에 반영하지 않는다")
    void process_issue_view_count_updated_message() throws Exception {
        // given
        String message = issueViewCountUpdatedMessage("event-view-count-1", 10L, 150L);

        // when
        scoreProcessor.process(message);

        // then
        assertThat(inboxEventRepository.findByEventId("event-view-count-1")).isEmpty();
        assertThat(publicIssueCandidateRepository.count()).isZero();
        assertThat(publicIssueRankingSummaryRepository.count()).isZero();
        assertThat(redisTemplate.opsForZSet().score(dailyRankingKey(), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(weeklyRankingKey(), "10"))
                .isNull();
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_AVAILABLE은 Redis state를 갱신하고 ranking summary를 직접 생성하지 않는다")
    void process_available_updates_redis_state_without_creating_ranking_summary() throws Exception {
        // given
        String viewCountMessage = issueViewCountUpdatedMessage("event-view-count-before-available", 10L, 150L);
        String availableMessage = publicIssueAvailableMessage("event-available-after-view-count", 10L, 20L);

        // when
        scoreProcessor.process(viewCountMessage);
        processor.process(availableMessage, LIFECYCLE_QUEUE_NAME);

        // then
        assertThat(publicIssueRankingSummaryRepository.count()).isZero();
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "status"))
                .isEqualTo(PublicIssueCandidateStatus.AVAILABLE.name());
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "collected_at"))
                .isEqualTo("2050-06-05T00:59:00Z");
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED는 Redis state를 REMOVED로 갱신한다")
    void process_removed_updates_redis_state() throws Exception {
        // given
        String removedMessage = publicIssueRemovedMessage("event-removed-state", 10L);

        // when
        processor.process(removedMessage, LIFECYCLE_QUEUE_NAME);

        // then
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "status"))
                .isEqualTo(PublicIssueCandidateStatus.REMOVED.name());
        assertThat(redisTemplate.opsForHash().get(issueStateKey(10L), "collected_at"))
                .isEqualTo("2050-06-05T00:59:00Z");
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED 이후 늦은 조회수 이벤트는 Redis ranking을 되살리지 않는다")
    void process_view_count_after_removed_does_not_recreate_ranking_summary() throws Exception {
        // given
        processor.process(publicIssueAvailableMessage("event-avail-before-late-view", 10L, 20L),
                LIFECYCLE_QUEUE_NAME);
        scoreProcessor.process(issueViewCountUpdatedMessage("event-view-before-removed", 10L, 150L));
        processor.process(publicIssueRemovedMessage("event-rem-before-late-view", 10L),
                LIFECYCLE_QUEUE_NAME);

        // when
        scoreProcessor.process(issueViewCountUpdatedMessage("event-view-after-removed", 10L, 200L));

        // then
        PublicIssueCandidate candidate = publicIssueCandidateRepository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);

        assertThat(publicIssueRankingSummaryRepository.count()).isZero();
        assertThat(redisTemplate.opsForZSet().score(dailyRankingKey(), "10"))
                .isNull();
        assertThat(redisTemplate.opsForZSet().score(weeklyRankingKey(), "10"))
                .isNull();
    }

    @Test
    @DisplayName("낮은 조회수 이벤트가 늦게 도착해도 Redis ranking score를 낮추지 않는다")
    void process_stale_view_count_does_not_decrease_ranking_summary() throws Exception {
        // given
        processor.process(publicIssueAvailableMessage("event-available-before-stale-view", 10L, 20L),
                LIFECYCLE_QUEUE_NAME);
        scoreProcessor.process(issueViewCountUpdatedMessage("event-view-count-high", 10L, 200L));

        // when
        scoreProcessor.process(issueViewCountUpdatedMessage("event-view-count-stale", 10L, 150L));

        // then
        assertThat(publicIssueRankingSummaryRepository.count()).isZero();
        assertThat(redisTemplate.opsForZSet().score(dailyRankingKey(), "10"))
                .isEqualTo(200.0);
        assertThat(redisTemplate.opsForZSet().score(weeklyRankingKey(), "10"))
                .isEqualTo(200.0);
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED 메시지를 처리하면 inbox를 완료 처리하고 공개 이슈 후보를 REMOVED 상태로 저장한다")
    void process_public_issue_removed_message() throws Exception {
        // given
        String message = publicIssueRemovedMessage("event-removed-1", 10L);

        // when
        processor.process(message, LIFECYCLE_QUEUE_NAME);

        // then
        InboxEvent inboxEvent = inboxEventRepository.findByEventId("event-removed-1").orElseThrow();
        assertThat(inboxEvent.getEventType()).isEqualTo(TrendingEventType.PUBLIC_ISSUE_REMOVED.value());
        assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEvent.getProcessedAt()).isNotNull();
        assertThat(inboxEvent.getQueueName()).isEqualTo(LIFECYCLE_QUEUE_NAME);
        JsonNode storedPayload = objectMapper.readTree(inboxEvent.getPayload());
        assertThat(storedPayload.path("issueId").asLong()).isEqualTo(10L);

        PublicIssueCandidate candidate = publicIssueCandidateRepository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isNull();
        assertThat(candidate.getCategory()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);
        assertThat(candidate.getPublicFeedCollectedAt()).isNull();
    }

    @Test
    @DisplayName("PUBLIC_ISSUE_REMOVED가 먼저 처리되면 늦은 PUBLIC_ISSUE_AVAILABLE은 후보를 되살리지 않는다")
    void process_public_issue_removed_before_late_available_message() throws Exception {
        // given
        String removedMessage = publicIssueRemovedMessage("event-removed-2", 10L);
        String availableMessage = publicIssueAvailableMessage("event-available-late", 10L, 20L);

        // when
        processor.process(removedMessage, LIFECYCLE_QUEUE_NAME);
        processor.process(availableMessage, LIFECYCLE_QUEUE_NAME);

        // then
        assertThat(inboxEventRepository.findByEventId("event-removed-2").orElseThrow().getStatus())
                .isEqualTo(InboxEventStatus.PROCESSED);
        assertThat(inboxEventRepository.findByEventId("event-available-late").orElseThrow().getStatus())
                .isEqualTo(InboxEventStatus.PROCESSED);

        PublicIssueCandidate candidate = publicIssueCandidateRepository.findByIssueId(10L).orElseThrow();
        assertThat(candidate.getNewsletterId()).isNull();
        assertThat(candidate.getCategory()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(PublicIssueCandidateStatus.REMOVED);
        assertThat(candidate.getPublicFeedCollectedAt()).isNull();
    }

    @Test
    @DisplayName("이미 처리된 eventId가 다시 오면 공개 이슈 후보를 중복 저장하지 않는다")
    void skip_already_processed_event_id() throws Exception {
        // given
        String message = publicIssueAvailableMessage("event-2", 10L, 20L);
        processor.process(message, LIFECYCLE_QUEUE_NAME);

        // when
        processor.process(message, LIFECYCLE_QUEUE_NAME);

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
        processor.process(firstMessage, LIFECYCLE_QUEUE_NAME);
        processor.process(secondMessage, LIFECYCLE_QUEUE_NAME);

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
        assertThatThrownBy(() -> processor.process(message, LIFECYCLE_QUEUE_NAME))
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

    private String publicIssueRemovedMessage(String eventId, Long issueId) throws Exception {
        return message(
                eventId,
                TrendingEventType.PUBLIC_ISSUE_REMOVED.value(),
                2,
                new PublicIssueRemovedPayload(issueId, Instant.parse("2050-06-05T00:59:00Z"))
        );
    }

    private String issueViewCountUpdatedMessage(String eventId, Long issueId, Long viewCount) throws Exception {
        return message(
                eventId,
                TrendingEventType.ISSUE_VIEW_COUNT_UPDATED.value(),
                new IssueViewCountUpdatedPayload(
                        issueId,
                        viewCount
                )
        );
    }

    private String issueStateKey(Long issueId) {
        return String.join(":", "letterpick:trending:issue", "{" + issueId + "}", "state");
    }

    private String dailyRankingKey() {
        return "letterpick:trending:ranking:{DAILY:2050-06-05}:issues";
    }

    private String weeklyRankingKey() {
        return "letterpick:trending:ranking:{WEEKLY:2050-05-30}:issues";
    }

    private String message(String eventId, String eventType, Object payload) throws Exception {
        return message(eventId, eventType, 1, payload);
    }

    private String message(String eventId, String eventType, int schemaVersion, Object payload) throws Exception {
        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                schemaVersion,
                "letterpick",
                Instant.parse("2050-06-05T01:00:00Z"),
                "trace-1",
                payload
        );
        return objectMapper.writeValueAsString(envelope);
    }
}
