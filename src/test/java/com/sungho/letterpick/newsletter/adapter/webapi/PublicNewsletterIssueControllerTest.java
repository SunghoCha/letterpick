package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecordRequest;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecorder;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicNewsletterIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, PublicIssueViewActorArgumentResolverRegistrar.class})
class PublicNewsletterIssueControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicNewsletterIssueFinder publicNewsletterIssueFinder;

    @MockitoBean
    PublicNewsletterIssueViewCountRecorder publicNewsletterIssueViewCountRecorder;

    @MockitoBean
    PublicIssueViewActorResolver publicIssueViewActorResolver;

    @Test
    @DisplayName("GET /api/v1/newsletter-issues 요청 시 검색 조건과 페이지 조건을 바인딩하고 공개 피드 목록을 반환한다")
    void getIssues_binds_search_condition_and_pageable_then_returns_public_issue_page_response() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 2);
        List<NewsletterIssueItem> issues = List.of(
                new NewsletterIssueItem(
                        10L,
                        1L,
                        "테크 레터",
                        "https://example.com/tech.png",
                        NewsletterCategory.TECH,
                        "테크 뉴스",
                        "테크 뉴스 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z"),
                        false
                )
        );
        given(publicNewsletterIssueFinder.findIssues(any(), any()))
                .willReturn(new SliceImpl<>(issues, pageable, true));

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues")
                        .param("category", "TECH")
                        .param("keyword", "redis")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].issueId").value(10L))
                .andExpect(jsonPath("$.items[0].newsletterId").value(1L))
                .andExpect(jsonPath("$.items[0].newsletterName").value("테크 레터"))
                .andExpect(jsonPath("$.items[0].newsletterImageUrl").value("https://example.com/tech.png"))
                .andExpect(jsonPath("$.items[0].newsletterCategory.code").value("TECH"))
                .andExpect(jsonPath("$.items[0].newsletterCategory.label").value("IT·테크"))
                .andExpect(jsonPath("$.items[0].subject").value("테크 뉴스"))
                .andExpect(jsonPath("$.items[0].previewText").value("테크 뉴스 미리보기"))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        ArgumentCaptor<PublicNewsletterIssueSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(PublicNewsletterIssueSearchCondition.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(publicNewsletterIssueFinder).findIssues(conditionCaptor.capture(), pageableCaptor.capture());

        assertThat(conditionCaptor.getValue().category()).isEqualTo(NewsletterCategory.TECH);
        assertThat(conditionCaptor.getValue().keyword()).isEqualTo("redis");
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/v1/newsletter-issues 요청 시 존재하지 않는 카테고리 값이면 400이 반환된다")
    void getIssues_returns_400_when_category_is_unknown() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues")
                        .param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(containsString("category")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(publicNewsletterIssueFinder);
    }

    @Test
    @DisplayName("GET /api/v1/newsletter-issues/rankings 요청 시 인기 이슈 목록을 반환한다")
    void getIssueRankings_returns_public_issue_rankings() throws Exception {
        // given
        given(publicNewsletterIssueFinder.findRankings(PublicIssueRankingWindowType.DAILY, 3))
                .willReturn(List.of(new PublicNewsletterIssueRankingItem(
                        10L,
                        1L,
                        "테크 레터",
                        "https://example.com/tech.png",
                        NewsletterCategory.TECH,
                        "테크 뉴스",
                        "테크 뉴스 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z"),
                        123
                )));

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues/rankings")
                        .param("windowType", "DAILY")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].issueId").value(10L))
                .andExpect(jsonPath("$.items[0].newsletterId").value(1L))
                .andExpect(jsonPath("$.items[0].newsletterName").value("테크 레터"))
                .andExpect(jsonPath("$.items[0].newsletterImageUrl").value("https://example.com/tech.png"))
                .andExpect(jsonPath("$.items[0].newsletterCategory.code").value("TECH"))
                .andExpect(jsonPath("$.items[0].newsletterCategory.label").value("IT·테크"))
                .andExpect(jsonPath("$.items[0].subject").value("테크 뉴스"))
                .andExpect(jsonPath("$.items[0].previewText").value("테크 뉴스 미리보기"))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].score").value(123));

        verify(publicNewsletterIssueFinder).findRankings(PublicIssueRankingWindowType.DAILY, 3);
    }

    @Test
    @DisplayName("GET /api/v1/newsletter-issues/{issueId} 요청 시 공개 이슈 상세 응답을 반환한다")
    void getIssueDetail_returns_200_and_public_issue_detail_response() throws Exception {
        // given
        NewsletterIssueDetail detail = new NewsletterIssueDetail(
                10L,
                1L,
                "테크 레터",
                "https://example.com/tech.png",
                "테크 뉴스",
                "<p>테크 뉴스 본문</p>",
                Instant.parse("2050-05-12T01:00:00Z"),
                true
        );
        given(publicNewsletterIssueFinder.findIssueDetail(10L))
                .willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues/{issueId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(10L))
                .andExpect(jsonPath("$.newsletterId").value(1L))
                .andExpect(jsonPath("$.newsletterName").value("테크 레터"))
                .andExpect(jsonPath("$.newsletterImageUrl").value("https://example.com/tech.png"))
                .andExpect(jsonPath("$.subject").value("테크 뉴스"))
                .andExpect(jsonPath("$.content").value("<p>테크 뉴스 본문</p>"))
                .andExpect(jsonPath("$.receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.read").doesNotExist());

        verify(publicNewsletterIssueFinder).findIssueDetail(10L);
    }

    @Test
    @DisplayName("GET /api/v1/newsletter-issues/{issueId} 요청 시 이슈를 찾지 못하면 404가 반환된다")
    void getIssueDetail_returns_404_when_issue_not_found() throws Exception {
        // given
        given(publicNewsletterIssueFinder.findIssueDetail(999L))
                .willThrow(new NewsletterIssueNotFoundException());

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues/{issueId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NWL-003"));

        verify(publicNewsletterIssueFinder).findIssueDetail(999L);
    }

    @Test
    @DisplayName("POST /api/v1/newsletter-issues/{issueId}/views 요청 시 actorKey를 해석하고 조회수 기록을 위임한다")
    void recordIssueView_resolves_actor_key_and_records_view_count() throws Exception {
        // given
        given(publicIssueViewActorResolver.resolveActorKey(any(), any(), any()))
                .willReturn("anonymous:visitor-1");

        // when & then
        mockMvc.perform(post("/api/v1/newsletter-issues/{issueId}/views", 10L))
                .andExpect(status().isNoContent());

        verify(publicIssueViewActorResolver).resolveActorKey(any(), any(), any());
        verify(publicNewsletterIssueViewCountRecorder).record(new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "anonymous:visitor-1"
        ));
    }
}
