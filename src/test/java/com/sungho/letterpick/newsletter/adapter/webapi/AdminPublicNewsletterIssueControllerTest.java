package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import com.sungho.letterpick.newsletter.domain.exception.NewsletterIssueNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPublicNewsletterIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPublicNewsletterIssueControllerTest {

    private static final String PATH = "/api/v1/admin/public-newsletter-issues/{issueId}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicNewsletterIssueModifier publicNewsletterIssueModifier;

    @Test
    @DisplayName("DELETE /api/v1/admin/public-newsletter-issues/{issueId} 요청 시 공개 피드 이슈 삭제를 위임한다")
    void deleteIssue_delegates_to_public_newsletter_issue_modifier() throws Exception {
        // when & then
        mockMvc.perform(delete(PATH, 10L))
                .andExpect(status().isNoContent());

        verify(publicNewsletterIssueModifier).delete(10L);
    }

    @Test
    @DisplayName("삭제할 공개 피드 이슈를 찾지 못하면 404를 반환한다")
    void deleteIssue_returns_404_when_public_issue_not_found() throws Exception {
        // given
        willThrow(new NewsletterIssueNotFoundException())
                .given(publicNewsletterIssueModifier).delete(999L);

        // when & then
        mockMvc.perform(delete(PATH, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NWL-003"));

        verify(publicNewsletterIssueModifier).delete(999L);
    }
}
