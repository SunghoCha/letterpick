package com.sungho.letterpick.newsletter.adapter.webapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungho.letterpick.common.config.WebMvcConfig;
import com.sungho.letterpick.newsletter.application.PublicIssueViewCountProperties;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecordRequest;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueViewCountRecorder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicNewsletterIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        WebMvcConfig.class,
        PublicIssueViewActorArgumentResolverRegistrar.class,
        PublicIssueViewActorResolver.class,
        PublicNewsletterIssueViewActorMvcTest.TestConfig.class
})
class PublicNewsletterIssueViewActorMvcTest {

    private static final String COOKIE_NAME = "letterpick_anonymous_id";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicNewsletterIssueFinder publicNewsletterIssueFinder;

    @MockitoBean
    PublicNewsletterIssueViewCountRecorder publicNewsletterIssueViewCountRecorder;

    @Test
    @DisplayName("anonymousId 쿠키가 없으면 MVC argument resolver가 쿠키를 발급하고 조회수 기록 actorKey로 전달한다")
    void recordIssueView_sets_anonymous_cookie_through_argument_resolver() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/newsletter-issues/{issueId}/views", 10L))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                        .contains(COOKIE_NAME + "=generated-anonymous-id")
                        .contains("Max-Age=7776000")
                        .contains("Path=/")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"));

        verify(publicNewsletterIssueViewCountRecorder).record(new PublicNewsletterIssueViewCountRecordRequest(
                10L,
                "anonymous:generated-anonymous-id"
        ));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        PublicIssueViewCountProperties publicIssueViewCountProperties() {
            return new PublicIssueViewCountProperties(
                    50,
                    Duration.ofMinutes(30),
                    "letterpick:public-issue",
                    COOKIE_NAME,
                    Duration.ofDays(90),
                    false
            );
        }

        @Bean
        AnonymousIdGenerator anonymousIdGenerator() {
            return () -> "generated-anonymous-id";
        }
    }
}
