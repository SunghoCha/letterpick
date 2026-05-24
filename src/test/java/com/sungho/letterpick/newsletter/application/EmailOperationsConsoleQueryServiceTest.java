package com.sungho.letterpick.newsletter.application;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECIPIENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailOperationsConsoleQueryServiceTest {

    @Mock
    private InboundEmailRepository inboundEmailRepository;

    @Test
    @DisplayName("현재 시각 기준 최근 24시간 인입 메일 상태 요약을 조회한다")
    void findStatusSummary_returns_recent_24_hours_status_summary_with_zero_filled_status_counts() {
        Instant now = Instant.parse("2050-05-12T03:00:00Z");
        Instant receivedFrom = Instant.parse("2050-05-11T03:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        EmailOperationsConsoleQueryService service =
                new EmailOperationsConsoleQueryService(inboundEmailRepository, clock);

        given(inboundEmailRepository.countByStatus(receivedFrom, now))
                .willReturn(List.of(
                        new InboundEmailStatusCount(ISSUE_CREATED, 2L),
                        new InboundEmailStatusCount(RECIPIENT_NOT_FOUND, 1L)
                ));

        InboundEmailStatusSummary result = service.findStatusSummary();

        verify(inboundEmailRepository).countByStatus(receivedFrom, now);
        assertThat(result.receivedFrom()).isEqualTo(receivedFrom);
        assertThat(result.receivedTo()).isEqualTo(now);
        assertThat(result.totalCount()).isEqualTo(3L);
        assertThat(result.statusCounts()).hasSize(InboundEmailStatus.values().length);
        assertThat(result.statusCounts()).contains(
                new InboundEmailStatusCount(ISSUE_CREATED, 2L),
                new InboundEmailStatusCount(RECIPIENT_NOT_FOUND, 1L),
                new InboundEmailStatusCount(RECEIVED, 0L)
        );
    }
}
