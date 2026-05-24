package com.sungho.letterpick.newsletter.adapter.webapi;

import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.ISSUE_CREATED;
import static com.sungho.letterpick.newsletter.domain.InboundEmailStatus.RECEIVED;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
}
