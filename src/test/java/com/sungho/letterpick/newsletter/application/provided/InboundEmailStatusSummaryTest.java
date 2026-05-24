package com.sungho.letterpick.newsletter.application.provided;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InboundEmailStatusSummaryTest {

    @Test
    @DisplayName("상태 요약 생성 시 수신 시각 범위가 역전되면 예외가 발생한다")
    void constructor_throws_exception_when_received_at_range_is_reversed() {
        Instant receivedFrom = Instant.parse("2050-05-12T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-11T15:00:00Z");

        assertThatThrownBy(() -> new InboundEmailStatusSummary(
                receivedFrom,
                receivedTo,
                0L,
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("receivedFrom must be before or equal to receivedTo");
    }

    @Test
    @DisplayName("상태 요약 생성 시 전체 건수가 음수이면 예외가 발생한다")
    void constructor_throws_exception_when_total_count_is_negative() {
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        assertThatThrownBy(() -> new InboundEmailStatusSummary(
                receivedFrom,
                receivedTo,
                -1L,
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalCount must be non-negative");
    }

    @Test
    @DisplayName("상태 요약 생성 시 상태별 건수가 음수이면 예외가 발생한다")
    void constructor_throws_exception_when_status_count_is_negative() {
        Instant receivedFrom = Instant.parse("2050-05-11T15:00:00Z");
        Instant receivedTo = Instant.parse("2050-05-12T15:00:00Z");

        assertThatThrownBy(() -> new InboundEmailStatusSummary(
                receivedFrom,
                receivedTo,
                0L,
                List.of(new InboundEmailStatusCount(RECEIVED, -1L))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status count must be non-negative");
    }
}
