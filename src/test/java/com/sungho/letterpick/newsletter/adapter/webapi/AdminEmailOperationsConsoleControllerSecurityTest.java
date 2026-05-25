package com.sungho.letterpick.newsletter.adapter.webapi;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.common.auth.WithAdminUser;
import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.SecurityConfig;
import com.sungho.letterpick.member.adapter.security.CustomOAuth2UserService;
import com.sungho.letterpick.member.adapter.security.CustomOidcUserService;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginFailureHandler;
import com.sungho.letterpick.member.adapter.security.OAuth2LoginSuccessHandler;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsQueueStatus;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminEmailOperationsConsoleController.class)
@Import(SecurityConfig.class)
class AdminEmailOperationsConsoleControllerSecurityTest {

    private static final String STATUS_SUMMARY_PATH = "/api/v1/admin/email-operations/status-summary";
    private static final String ACTION_REQUIRED_PATH = "/api/v1/admin/email-operations/action-required";
    private static final String STALE_RECEIVED_PATH = "/api/v1/admin/email-operations/stale-received";
    private static final String QUEUE_STATUS_PATH = "/api/v1/admin/email-operations/queue-status";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailOperationsConsoleFinder emailOperationsConsoleFinder;

    @MockitoBean
    CustomOidcUserService customOidcUserService;

    @MockitoBean
    CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @MockitoBean
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("ROLE_ADMIN 없는 사용자가 이메일 운영 콘솔 API 호출 시 403")
    void status_summary_returns_403_for_non_admin() throws Exception {
        mockMvc.perform(get(STATUS_SUMMARY_PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(emailOperationsConsoleFinder, never()).findStatusSummary();
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 이메일 운영 콘솔 API 호출 시 권한 통과")
    void status_summary_passes_for_admin() throws Exception {
        given(emailOperationsConsoleFinder.findStatusSummary())
                .willReturn(new InboundEmailStatusSummary(
                        Instant.parse("2050-05-11T03:00:00Z"),
                        Instant.parse("2050-05-12T03:00:00Z"),
                        0L,
                        List.of()
                ));

        mockMvc.perform(get(STATUS_SUMMARY_PATH))
                .andExpect(status().isOk());

        verify(emailOperationsConsoleFinder).findStatusSummary();
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("ROLE_ADMIN 없는 사용자가 조치 필요 목록 API 호출 시 403")
    void action_required_returns_403_for_non_admin() throws Exception {
        mockMvc.perform(get(ACTION_REQUIRED_PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(emailOperationsConsoleFinder, never()).findActionRequiredItems(any(Pageable.class));
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 조치 필요 목록 API 호출 시 권한 통과")
    void action_required_passes_for_admin() throws Exception {
        given(emailOperationsConsoleFinder.findActionRequiredItems(any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get(ACTION_REQUIRED_PATH))
                .andExpect(status().isOk());

        verify(emailOperationsConsoleFinder).findActionRequiredItems(any(Pageable.class));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("ROLE_ADMIN 없는 사용자가 처리 지연 목록 API 호출 시 403")
    void stale_received_returns_403_for_non_admin() throws Exception {
        mockMvc.perform(get(STALE_RECEIVED_PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(emailOperationsConsoleFinder, never()).findStaleReceivedItems(any(Pageable.class));
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 처리 지연 목록 API 호출 시 권한 통과")
    void stale_received_passes_for_admin() throws Exception {
        given(emailOperationsConsoleFinder.findStaleReceivedItems(any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get(STALE_RECEIVED_PATH))
                .andExpect(status().isOk());

        verify(emailOperationsConsoleFinder).findStaleReceivedItems(any(Pageable.class));
    }

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("ROLE_ADMIN 없는 사용자가 큐 상태 API 호출 시 403")
    void queue_status_returns_403_for_non_admin() throws Exception {
        mockMvc.perform(get(QUEUE_STATUS_PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(emailOperationsConsoleFinder, never()).findQueueStatus();
    }

    @Test
    @WithAdminUser
    @DisplayName("ROLE_ADMIN 있는 사용자가 큐 상태 API 호출 시 권한 통과")
    void queue_status_passes_for_admin() throws Exception {
        given(emailOperationsConsoleFinder.findQueueStatus())
                .willReturn(EmailOperationsQueueStatus.available(
                        Instant.parse("2050-05-12T03:00:00Z"),
                        new EmailOperationsQueueStatus.MainQueueSnapshot(0L, 0L, 0L),
                        new EmailOperationsQueueStatus.DeadLetterQueueSnapshot(0L)
                ));

        mockMvc.perform(get(QUEUE_STATUS_PATH))
                .andExpect(status().isOk());

        verify(emailOperationsConsoleFinder).findQueueStatus();
    }
}
