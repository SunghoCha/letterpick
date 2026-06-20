package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.common.config.SecurityConfig;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.member.adapter.security.CustomOAuth2UserService;
import com.sungho.letterpick.member.adapter.security.CustomOidcUserService;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginFailureHandler;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginSuccessHandler;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecordRequest;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecorder;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicNewsletterIssueController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfig.class,
        PublicIssueViewActorArgumentResolverRegistrar.class
})
class PublicNewsletterIssueControllerSecurityTest {

    private static final String VIEW_COUNT_PATH = "/api/v1/newsletter-issues/{issueId}/views";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicNewsletterIssueFinder publicNewsletterIssueFinder;

    @MockitoBean
    PublicNewsletterIssueViewCountRecorder publicNewsletterIssueViewCountRecorder;

    @MockitoBean
    PublicIssueViewActorResolver publicIssueViewActorResolver;

    @MockitoBean
    CustomOidcUserService customOidcUserService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @DisplayName("익명 사용자가 공개 뉴스레터 피드 목록을 조회할 수 있다")
    void getIssues_passes_for_anonymous() throws Exception {
        // given
        given(publicNewsletterIssueFinder.findIssues(any(), any()))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues"))
                .andExpect(status().isOk());

        verify(publicNewsletterIssueFinder).findIssues(any(), any());
    }

    @Test
    @DisplayName("익명 사용자가 공개 뉴스레터 인기 이슈를 조회할 수 있다")
    void getIssueRankings_passes_for_anonymous() throws Exception {
        // given
        given(publicNewsletterIssueFinder.findRankings(PublicIssueRankingWindowType.DAILY, 20))
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
                        .param("limit", "20"))
                .andExpect(status().isOk());

        verify(publicNewsletterIssueFinder).findRankings(PublicIssueRankingWindowType.DAILY, 20);
    }

    @Test
    @DisplayName("익명 사용자가 공개 뉴스레터 이슈 상세를 조회할 수 있다")
    void getIssueDetail_passes_for_anonymous() throws Exception {
        // given
        given(publicNewsletterIssueFinder.findIssueDetail(10L))
                .willReturn(new NewsletterIssueDetail(
                        10L,
                        1L,
                        "테크 레터",
                        "https://example.com/tech.png",
                        "테크 뉴스",
                        "<p>테크 뉴스 본문</p>",
                        Instant.parse("2050-05-12T01:00:00Z"),
                        true
                ));

        // when & then
        mockMvc.perform(get("/api/v1/newsletter-issues/{issueId}", 10L))
                .andExpect(status().isOk());

        verify(publicNewsletterIssueFinder).findIssueDetail(10L);
    }

    @Test
    @DisplayName("익명 사용자가 CSRF와 함께 공개 이슈 조회수 기록을 요청할 수 있다")
    void recordIssueView_passes_for_anonymous_with_csrf() throws Exception {
        // given
        given(publicIssueViewActorResolver.resolveActorKey(any(), any(), any()))
                .willReturn("anonymous:visitor-1");

        // when & then
        mockMvc.perform(post(VIEW_COUNT_PATH, 10L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(publicNewsletterIssueViewCountRecorder).record(new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "anonymous:visitor-1"
        ));
    }

    @Test
    @DisplayName("익명 사용자가 CSRF 없이 공개 이슈 조회수 기록을 요청하면 403")
    void recordIssueView_returns_403_for_anonymous_without_csrf() throws Exception {
        // when & then
        mockMvc.perform(post(VIEW_COUNT_PATH, 10L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(publicNewsletterIssueViewCountRecorder, publicIssueViewActorResolver);
    }
}
