package com.sungho.letterpick.newsletter.adapter.webapi.dev;

import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_PENDING_SIGNUP;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.SecurityConfig;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.member.adapter.security.CustomOAuth2UserService;
import com.sungho.letterpick.member.adapter.security.CustomOidcUserService;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginFailureHandler;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginSuccessHandler;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoResult;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevNewsletterIssueDemoController.class)
@ActiveProfiles("dev")
@Import({SecurityConfig.class, WebMvcConfig.class})
class DevNewsletterIssueDemoControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DevNewsletterIssueDemoService demoService;

    @MockitoBean
    CustomOidcUserService customOidcUserService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @DisplayName("익명 사용자가 데모 뉴스레터 이슈 생성 시 401")
    void createDemoIssues_returns_401_for_anonymous() throws Exception {
        mockMvc.perform(post("/api/v1/me/newsletter-issues/demo")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(demoService);
    }

    @Test
    @WithLoginUser(memberId = 42L, authorities = {ROLE_PENDING_SIGNUP})
    @DisplayName("가입 대기 사용자가 데모 뉴스레터 이슈 생성 시 403")
    void createDemoIssues_returns_403_for_pending_signup_user() throws Exception {
        mockMvc.perform(post("/api/v1/me/newsletter-issues/demo")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(demoService);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("인증 사용자가 CSRF 없이 데모 뉴스레터 이슈 생성 시 403")
    void createDemoIssues_returns_403_when_csrf_missing() throws Exception {
        mockMvc.perform(post("/api/v1/me/newsletter-issues/demo"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(demoService);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("인증 사용자가 CSRF와 함께 데모 뉴스레터 이슈 생성 시 권한 통과")
    void createDemoIssues_passes_for_authenticated() throws Exception {
        given(demoService.createFor(42L))
                .willReturn(new DevNewsletterIssueDemoResult(8, 0));

        mockMvc.perform(post("/api/v1/me/newsletter-issues/demo")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(demoService).createFor(42L);
    }
}
