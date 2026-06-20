package com.sungho.letterpick.trending.ranking.adapter.webapi;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowType;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicIssueRankingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicIssueRankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicIssueRankingFinder publicIssueRankingFinder;

    @Test
    @DisplayName("GET /internal/api/v1/public-issue-rankings 요청 시 지정한 windowType의 인기 이슈 순위를 반환한다")
    void get_public_issue_rankings() throws Exception {
        // given
        given(publicIssueRankingFinder.findTop(PublicIssueRankingWindowType.DAILY, 2))
                .willReturn(List.of(
                        new PublicIssueRankingItem(40L, 999L),
                        new PublicIssueRankingItem(10L, 120L)
                ));

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings")
                        .param("windowType", "DAILY")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].issueId").value(40L))
                .andExpect(jsonPath("$.items[0].score").value(999L))
                .andExpect(jsonPath("$.items[0].viewCount").doesNotExist())
                .andExpect(jsonPath("$.items[1].issueId").value(10L))
                .andExpect(jsonPath("$.items[1].score").value(120L))
                .andExpect(jsonPath("$.items[1].viewCount").doesNotExist());

        verify(publicIssueRankingFinder).findTop(PublicIssueRankingWindowType.DAILY, 2);
    }

    @Test
    @DisplayName("limit이 1보다 작아도 finder에 그대로 전달한다")
    void pass_non_positive_limit_to_finder() throws Exception {
        // given
        given(publicIssueRankingFinder.findTop(PublicIssueRankingWindowType.DAILY, 0))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings")
                        .param("windowType", "DAILY")
                        .param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(publicIssueRankingFinder).findTop(PublicIssueRankingWindowType.DAILY, 0);
    }

    @Test
    @DisplayName("limit이 없으면 finder에 null로 전달한다")
    void pass_null_limit_to_finder() throws Exception {
        // given
        given(publicIssueRankingFinder.findTop(PublicIssueRankingWindowType.WEEKLY, null))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings")
                        .param("windowType", "WEEKLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(publicIssueRankingFinder).findTop(PublicIssueRankingWindowType.WEEKLY, null);
    }

    @Test
    @DisplayName("limit이 maxSize보다 커도 finder에 그대로 전달한다")
    void pass_too_large_limit_to_finder() throws Exception {
        // given
        given(publicIssueRankingFinder.findTop(PublicIssueRankingWindowType.DAILY, 101))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings")
                        .param("windowType", "DAILY")
                        .param("limit", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(publicIssueRankingFinder).findTop(PublicIssueRankingWindowType.DAILY, 101);
    }
}
