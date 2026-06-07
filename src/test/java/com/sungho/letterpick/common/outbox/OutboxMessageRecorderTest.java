package com.sungho.letterpick.common.outbox;

import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.common.logging.MdcInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import com.sungho.letterpick.LetterPickDataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@LetterPickDataJpaTest
@ActiveProfiles("test")
@Import(LetterPickTestConfiguration.class)
class OutboxMessageRecorderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2050-06-06T01:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @AfterEach
    void tearDown() {
        MDC.remove(MdcInterceptor.REQUEST_ID);
    }

    @Test
    void recordsOutboxMessage() {
        OutboxMessageRecorder recorder = new OutboxMessageRecorder(
                outboxMessageRepository,
                new ObjectMapper(),
                CLOCK,
                new OutboxQueueNameResolver("letterpick-test-trending-lifecycle-events")
        );
        Instant occurredAt = Instant.parse("2050-06-06T00:59:00Z");
        MDC.put(MdcInterceptor.REQUEST_ID, "trace-1");

        recorder.record(new OutboxMessageRecordRequest(
                "event-1",
                OutboxMessageType.PUBLIC_ISSUE_AVAILABLE,
                "1",
                new SamplePayload(1L, "TECH"),
                occurredAt
        ));

        OutboxMessage saved = outboxMessageRepository.findAll().getFirst();
        assertThat(saved.getEventId()).isEqualTo("event-1");
        assertThat(saved.getDestination()).isEqualTo("letterpick-test-trending-lifecycle-events");
        assertThat(saved.getEventType()).isEqualTo("PUBLIC_ISSUE_AVAILABLE");
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
        assertThat(saved.getSource()).isEqualTo("letterpick");
        assertThat(saved.getAggregateType()).isEqualTo("NEWSLETTER_ISSUE");
        assertThat(saved.getAggregateId()).isEqualTo("1");
        assertThat(saved.getPayload()).contains("\"issueId\":1", "\"category\":\"TECH\"");
        assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getNextAttemptAt()).isEqualTo(CLOCK.instant().plus(Duration.ofMinutes(1)));
        assertThat(saved.getCreatedAt()).isEqualTo(CLOCK.instant());
        assertThat(saved.getUpdatedAt()).isEqualTo(CLOCK.instant());
    }

    private record SamplePayload(Long issueId, String category) {
    }
}
