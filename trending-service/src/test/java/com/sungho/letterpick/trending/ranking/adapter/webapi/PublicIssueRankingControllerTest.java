package com.sungho.letterpick.trending.ranking.adapter.webapi;

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
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("GET /internal/api/v1/public-issue-rankings/today 요청 시 오늘 인기 이슈 순위를 반환한다")
    void get_today_public_issue_rankings() throws Exception {
        // given
        given(publicIssueRankingFinder.findTodayTop(2))
                .willReturn(List.of(
                        new PublicIssueRankingItem(40L, 999L, 999L),
                        new PublicIssueRankingItem(10L, 120L, 120L)
                ));

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings/today")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].issueId").value(40L))
                .andExpect(jsonPath("$.items[0].score").value(999L))
                .andExpect(jsonPath("$.items[0].viewCount").value(999L))
                .andExpect(jsonPath("$.items[1].issueId").value(10L))
                .andExpect(jsonPath("$.items[1].score").value(120L))
                .andExpect(jsonPath("$.items[1].viewCount").value(120L));

        verify(publicIssueRankingFinder).findTodayTop(2);
    }

    @Test
    @DisplayName("limit이 1보다 작으면 오늘 인기 이슈를 조회하지 않는다")
    void reject_non_positive_limit() throws Exception {
        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings/today")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicIssueRankingFinder);
    }

    @Test
    @DisplayName("limit이 없으면 기본 20개 기준으로 오늘 인기 이슈를 조회한다")
    void get_today_public_issue_rankings_with_default_limit() throws Exception {
        // given
        given(publicIssueRankingFinder.findTodayTop(20))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        verify(publicIssueRankingFinder).findTodayTop(20);
    }

    @Test
    @DisplayName("limit이 100보다 크면 오늘 인기 이슈를 조회하지 않는다")
    void reject_too_large_limit() throws Exception {
        // when & then
        mockMvc.perform(get("/internal/api/v1/public-issue-rankings/today")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicIssueRankingFinder);
    }
}
