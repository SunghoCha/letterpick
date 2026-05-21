package com.sungho.letterpick.newsletter.adapter.webapi.dev;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.common.auth.WithLoginUser;
import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoResult;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevNewsletterIssueDemoController.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcConfig.class)
class DevNewsletterIssueDemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DevNewsletterIssueDemoService demoService;

    @Test
    @WithLoginUser(memberId = 42L)
    @DisplayName("현재 로그인 회원 기준으로 데모 뉴스레터 이슈 생성을 요청한다")
    void createDemoIssues_delegates_current_member_id() throws Exception {
        given(demoService.createFor(42L))
                .willReturn(new DevNewsletterIssueDemoResult(8, 0));

        mockMvc.perform(post("/api/v1/me/newsletter-issues/demo")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdIssueCount").value(8))
                .andExpect(jsonPath("$.skippedIssueCount").value(0));

        verify(demoService).createFor(42L);
    }
}
