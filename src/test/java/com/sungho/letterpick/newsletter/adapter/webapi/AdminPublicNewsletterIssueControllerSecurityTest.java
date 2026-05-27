package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.common.auth.WithAdminUser;
import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.SecurityConfig;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.member.adapter.security.CustomOAuth2UserService;
import com.sungho.letterpick.member.adapter.security.CustomOidcUserService;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginFailureHandler;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginSuccessHandler;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPublicNewsletterIssueController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class AdminPublicNewsletterIssueControllerSecurityTest {

    private static final String PATH = "/api/v1/admin/public-newsletter-issues/{issueId}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicNewsletterIssueModifier publicNewsletterIssueModifier;

    @MockitoBean
    CustomOidcUserService customOidcUserService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @DisplayName("익명 사용자가 공개 피드 이슈 삭제 시 401")
    void deleteIssue_returns_401_for_anonymous() throws Exception {
        mockMvc.perform(delete(PATH, 10L)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(publicNewsletterIssueModifier);
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("ROLE_ADMIN 없는 사용자가 공개 피드 이슈 삭제 시 403")
    void deleteIssue_returns_403_for_non_admin() throws Exception {
        mockMvc.perform(delete(PATH, 10L)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(publicNewsletterIssueModifier, never()).delete(10L);
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 CSRF 없이 공개 피드 이슈 삭제 시 403")
    void deleteIssue_returns_403_when_csrf_missing() throws Exception {
        mockMvc.perform(delete(PATH, 10L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(publicNewsletterIssueModifier);
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 공개 피드 이슈 삭제 시 권한 통과")
    void deleteIssue_passes_for_admin() throws Exception {
        mockMvc.perform(delete(PATH, 10L)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(publicNewsletterIssueModifier).delete(10L);
    }
}
