package com.sungho.letterpick.newsletter.application;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.NEWSLETTER_NOT_FOUND;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECIPIENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailOperationsConsoleQueryServiceTest {

    @Mock
    private InboundEmailRepository inboundEmailRepository;

    @Test
    @DisplayName("현재 시각 기준 최근 24시간 인입 메일 상태 요약을 조회한다")
    void findStatusSummary_returns_recent_24_hours_status_summary_with_zero_filled_status_counts() {
        // given
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

        // when
        InboundEmailStatusSummary result = service.findStatusSummary();

        // then
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

    @Test
    @DisplayName("현재 시각 기준 최근 24시간 조치 필요 인입 메일 목록을 조회한다")
    void findActionRequired_returns_recent_24_hours_action_required_items() {
        // given
        Instant now = Instant.parse("2050-05-12T03:00:00Z");
        Instant receivedFrom = Instant.parse("2050-05-11T03:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        PageRequest pageable = PageRequest.of(0, 20);

        EmailOperationsConsoleQueryService service =
                new EmailOperationsConsoleQueryService(inboundEmailRepository, clock);
        Slice<InboundEmailAdminItem> expected = new SliceImpl<>(
                List.of(new InboundEmailAdminItem(
                        1L,
                        Instant.parse("2050-05-12T01:00:00Z"),
                        NEWSLETTER_NOT_FOUND,
                        "sender@example.com",
                        "recipient@inbound.letterpick.test",
                        "subject",
                        42L,
                        null,
                        "message-key",
                        "raw/message-key"
                )),
                pageable,
                false
        );
        given(inboundEmailRepository.findActionRequired(receivedFrom, now, pageable))
                .willReturn(expected);

        // when
        Slice<InboundEmailAdminItem> result = service.findActionRequiredItems(pageable);

        // then
        verify(inboundEmailRepository).findActionRequired(receivedFrom, now, pageable);
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("현재 시각 기준 10분 이상 RECEIVED로 남은 인입 메일 목록을 조회한다")
    void findStaleReceived_returns_received_items_older_than_10_minutes() {
        // given
        Instant now = Instant.parse("2050-05-12T03:00:00Z");
        Instant receivedBefore = Instant.parse("2050-05-12T02:50:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        PageRequest pageable = PageRequest.of(0, 20);
        EmailOperationsConsoleQueryService service =
                new EmailOperationsConsoleQueryService(inboundEmailRepository, clock);
        Slice<InboundEmailAdminItem> expected = new SliceImpl<>(
                List.of(new InboundEmailAdminItem(
                        1L,
                        Instant.parse("2050-05-12T02:49:00Z"),
                        RECEIVED,
                        "sender@example.com",
                        "recipient@inbound.letterpick.test",
                        "subject",
                        null,
                        null,
                        "message-key",
                        "raw/message-key"
                )),
                pageable,
                false
        );
        given(inboundEmailRepository.findStaleReceived(receivedBefore, pageable))
                .willReturn(expected);

        // when
        Slice<InboundEmailAdminItem> result = service.findStaleReceivedItems(pageable);

        // then
        verify(inboundEmailRepository).findStaleReceived(receivedBefore, pageable);
        assertThat(result).isSameAs(expected);
    }
}
