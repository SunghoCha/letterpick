package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueModifier;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsletterIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcConfig.class)
class NewsletterIssueControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NewsletterIssueFinder newsletterIssueFinder;

    @MockitoBean
    NewsletterIssueModifier newsletterIssueModifier;

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("오늘 도착한 뉴스레터 이슈 조회 요청 시 200과 이슈 목록 페이지 응답이 반환된다")
    void getTodayIssues_returns_200_and_newsletter_issue_page_response() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        List<NewsletterIssueItem> issues = List.of(
                new NewsletterIssueItem(
                        10L,
                        1L,
                        "Example Letter",
                        "https://example.com/image.png",
                        NewsletterCategory.TECH,
                        "오늘의 뉴스레터",
                        "본문 미리보기",
                        Instant.parse("2050-05-12T01:00:00Z"),
                        false
                )
        );
        given(newsletterIssueFinder.findTodayIssues(eq(42L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(issues, pageable, true));

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].issueId").value(10L))
                .andExpect(jsonPath("$.items[0].newsletterId").value(1L))
                .andExpect(jsonPath("$.items[0].newsletterName").value("Example Letter"))
                .andExpect(jsonPath("$.items[0].newsletterImageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.items[0].subject").value("오늘의 뉴스레터"))
                .andExpect(jsonPath("$.items[0].previewText").value("본문 미리보기"))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(newsletterIssueFinder).findTodayIssues(eq(42L), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("오늘 도착한 뉴스레터 이슈가 없으면 빈 목록 페이지 응답이 반환된다")
    void getTodayIssues_returns_empty_items_when_today_issues_do_not_exist() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(newsletterIssueFinder.findTodayIssues(eq(42L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(false));

        verify(newsletterIssueFinder).findTodayIssues(eq(42L), any(Pageable.class));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("보관함 뉴스레터 이슈 조회 요청 시 200과 이슈 목록 페이지 응답이 반환된다")
    void getIssues_returns_200_and_newsletter_issue_page_response() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        List<NewsletterIssueItem> issues = List.of(
                new NewsletterIssueItem(
                        20L,
                        2L,
                        "Archive Letter",
                        "https://example.com/archive.png",
                        NewsletterCategory.BIZ,
                        "보관함 뉴스레터",
                        "보관함 미리보기",
                        Instant.parse("2050-05-10T01:00:00Z"),
                        true
                )
        );
        given(newsletterIssueFinder.findIssues(eq(42L), isNull(), any(Pageable.class)))
                .willReturn(new SliceImpl<>(issues, pageable, true));

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].issueId").value(20L))
                .andExpect(jsonPath("$.items[0].newsletterId").value(2L))
                .andExpect(jsonPath("$.items[0].newsletterName").value("Archive Letter"))
                .andExpect(jsonPath("$.items[0].newsletterImageUrl").value("https://example.com/archive.png"))
                .andExpect(jsonPath("$.items[0].subject").value("보관함 뉴스레터"))
                .andExpect(jsonPath("$.items[0].previewText").value("보관함 미리보기"))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2050-05-10T01:00:00Z"))
                .andExpect(jsonPath("$.items[0].read").value(true))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(newsletterIssueFinder).findIssues(eq(42L), isNull(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("보관함에 표시할 뉴스레터 이슈가 없으면 빈 목록 페이지 응답이 반환된다")
    void getIssues_returns_empty_items_when_archive_issues_do_not_exist() throws Exception {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(newsletterIssueFinder.findIssues(eq(42L), isNull(), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.hasNext").value(false));

        verify(newsletterIssueFinder).findIssues(eq(42L), isNull(), any(Pageable.class));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("보관함 뉴스레터 이슈 조회 시 keyword query parameter를 전달한다")
    void getIssues_passes_keyword_query_parameter() throws Exception {
        // given
        String keyword = "spring";
        PageRequest pageable = PageRequest.of(0, 20);
        given(newsletterIssueFinder.findIssues(eq(42L), eq(keyword), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues")
                        .queryParam("keyword", keyword))
                .andExpect(status().isOk());

        verify(newsletterIssueFinder).findIssues(eq(42L), eq(keyword), any(Pageable.class));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("뉴스레터 이슈 상세 조회 요청 시 200과 상세 응답이 반환된다")
    void getIssueDetail_returns_200_and_newsletter_issue_detail_response() throws Exception {
        // given
        NewsletterIssueDetail detail = new NewsletterIssueDetail(
                10L,
                1L,
                "Example Letter",
                "https://example.com/image.png",
                "뉴스레터 제목",
                "<p>뉴스레터 본문</p>",
                Instant.parse("2050-05-12T01:00:00Z"),
                true
        );
        given(newsletterIssueFinder.readIssueDetail(42L, 10L))
                .willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues/{issueId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(10L))
                .andExpect(jsonPath("$.newsletterId").value(1L))
                .andExpect(jsonPath("$.newsletterName").value("Example Letter"))
                .andExpect(jsonPath("$.newsletterImageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.subject").value("뉴스레터 제목"))
                .andExpect(jsonPath("$.content").value("<p>뉴스레터 본문</p>"))
                .andExpect(jsonPath("$.receivedAt").value("2050-05-12T01:00:00Z"))
                .andExpect(jsonPath("$.read").value(true));

        verify(newsletterIssueFinder).readIssueDetail(42L, 10L);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("뉴스레터 이슈 상세 조회 시 이슈를 찾지 못하면 404가 반환된다")
    void getIssueDetail_returns_404_when_newsletter_issue_not_found() throws Exception {
        // given
        given(newsletterIssueFinder.readIssueDetail(42L, 999L))
                .willThrow(new NewsletterIssueNotFoundException());

        // when & then
        mockMvc.perform(get("/api/v1/me/newsletter-issues/{issueId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NWL-003"));

        verify(newsletterIssueFinder).readIssueDetail(42L, 999L);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("뉴스레터 이슈 삭제 요청 시 204가 반환된다")
    void deleteIssue_returns_204_when_newsletter_issue_deleted() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/me/newsletter-issues/{issueId}", 10L))
                .andExpect(status().isNoContent());

        verify(newsletterIssueModifier).delete(42L, 10L);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("뉴스레터 이슈 삭제 시 이슈를 찾지 못하면 404가 반환된다")
    void deleteIssue_returns_404_when_newsletter_issue_not_found() throws Exception {
        // given
        willThrow(new NewsletterIssueNotFoundException())
                .given(newsletterIssueModifier)
                .delete(42L, 999L);

        // when & then
        mockMvc.perform(delete("/api/v1/me/newsletter-issues/{issueId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NWL-003"));

        verify(newsletterIssueModifier).delete(42L, 999L);
    }
}
