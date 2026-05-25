package com.sungho.letterpick.newsletter.adapter.webapi;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.NEWSLETTER_NOT_FOUND;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsQueueStatus;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminEmailOperationsConsoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEmailOperationsConsoleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailOperationsConsoleFinder emailOperationsConsoleFinder;

    @Test
    @DisplayName("관리자가 인입 메일 상태 요약 조회 시 200과 상태별 집계 응답이 반환된다")
    void getStatusSummary_returns_200_and_status_summary_response() throws Exception {
        InboundEmailStatusSummary summary = new InboundEmailStatusSummary(
                Instant.parse("2050-05-11T03:00:00Z"),
                Instant.parse("2050-05-12T03:00:00Z"),
                7L,
                List.of(
                        new InboundEmailStatusCount(RECEIVED, 4L),
                        new InboundEmailStatusCount(ISSUE_CREATED, 3L)
                )
        );
        given(emailOperationsConsoleFinder.findStatusSummary())
                .willReturn(summary);

        mockMvc.perform(get("/api/v1/admin/email-operations/status-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedFrom").value("2050-05-11T03:00:00Z"))
                .andExpect(jsonPath("$.receivedTo").value("2050-05-12T03:00:00Z"))
                .andExpect(jsonPath("$.totalCount").value(7L))
                .andExpect(jsonPath("$.statusCounts.length()").value(2))
                .andExpect(jsonPath("$.statusCounts[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.statusCounts[0].count").value(4L))
                .andExpect(jsonPath("$.statusCounts[1].status").value("ISSUE_CREATED"))
                .andExpect(jsonPath("$.statusCounts[1].count").value(3L));

        verify(emailOperationsConsoleFinder).findStatusSummary();
    }

    @Test
    @DisplayName("관리자가 조치 필요 인입 메일 목록 조회 시 200과 목록 응답이 반환된다")
    void getActionRequiredItems_returns_200_and_action_required_items_response() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        given(emailOperationsConsoleFinder.findActionRequiredItems(any(Pageable.class)))
                .willReturn(new SliceImpl<>(
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
                        true
                ));

        mockMvc.perform(get("/api/v1/admin/email-operations/action-required"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].inboundEmailId").value(1L))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].status").value("NEWSLETTER_NOT_FOUND"))
                .andExpect(jsonPath("$.items[0].senderEmail").value("sender@example.com"))
                .andExpect(jsonPath("$.items[0].recipientAddress").value("recipient@inbound.letterpick.test"))
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].memberId").value(42L))
                .andExpect(jsonPath("$.items[0].newsletterId").doesNotExist())
                .andExpect(jsonPath("$.items[0].messageKey").value("message-key"))
                .andExpect(jsonPath("$.items[0].rawReference").value("raw/message-key"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(emailOperationsConsoleFinder).findActionRequiredItems(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("관리자가 처리 지연 인입 메일 목록 조회 시 200과 목록 응답이 반환된다")
    void getStaleReceivedItems_returns_200_and_stale_received_items_response() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(emailOperationsConsoleFinder.findStaleReceivedItems(any(Pageable.class)))
                .willReturn(new SliceImpl<>(
                        List.of(new InboundEmailAdminItem(
                                1L,
                                Instant.parse("2050-05-12T01:00:00Z"),
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
                ));

        // when & then
        mockMvc.perform(get("/api/v1/admin/email-operations/stale-received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].inboundEmailId").value(1L))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].senderEmail").value("sender@example.com"))
                .andExpect(jsonPath("$.items[0].recipientAddress").value("recipient@inbound.letterpick.test"))
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].memberId").doesNotExist())
                .andExpect(jsonPath("$.items[0].newsletterId").doesNotExist())
                .andExpect(jsonPath("$.items[0].messageKey").value("message-key"))
                .andExpect(jsonPath("$.items[0].rawReference").value("raw/message-key"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(false));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(emailOperationsConsoleFinder).findStaleReceivedItems(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("관리자가 큐 상태 조회 시 200과 큐 카운트 응답이 반환된다")
    void getQueueStatus_returns_200_and_available_queue_status_response() throws Exception {
        // given
        given(emailOperationsConsoleFinder.findQueueStatus())
                .willReturn(EmailOperationsQueueStatus.available(
                        Instant.parse("2050-05-12T03:00:00Z"),
                        new EmailOperationsQueueStatus.MainQueueSnapshot(3L, 2L, 1L),
                        new EmailOperationsQueueStatus.DeadLetterQueueSnapshot(4L)
                ));

        // when & then
        mockMvc.perform(get("/api/v1/admin/email-operations/queue-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedAt").value("2050-05-12T03:00:00Z"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.mainQueue.availableMessageCount").value(3L))
                .andExpect(jsonPath("$.mainQueue.inFlightMessageCount").value(2L))
                .andExpect(jsonPath("$.mainQueue.delayedMessageCount").value(1L))
                .andExpect(jsonPath("$.deadLetterQueue.availableMessageCount").value(4L))
                .andExpect(jsonPath("$.failureReason").doesNotExist());

        verify(emailOperationsConsoleFinder).findQueueStatus();
    }

    @Test
    @DisplayName("관리자가 큐 상태 조회 시 SQS 조회 실패 상태도 200과 조회 불가 응답으로 반환된다")
    void getQueueStatus_returns_200_and_unavailable_queue_status_response() throws Exception {
        // given
        given(emailOperationsConsoleFinder.findQueueStatus())
                .willReturn(EmailOperationsQueueStatus.unavailable(
                        Instant.parse("2050-05-12T03:00:00Z"),
                        "SQS queue status unavailable"
                ));

        // when & then
        mockMvc.perform(get("/api/v1/admin/email-operations/queue-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedAt").value("2050-05-12T03:00:00Z"))
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.mainQueue").doesNotExist())
                .andExpect(jsonPath("$.deadLetterQueue").doesNotExist())
                .andExpect(jsonPath("$.failureReason").value("SQS queue status unavailable"));

        verify(emailOperationsConsoleFinder).findQueueStatus();
    }
}
