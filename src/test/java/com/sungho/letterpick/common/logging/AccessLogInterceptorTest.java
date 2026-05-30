package com.sungho.letterpick.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

@ExtendWith(OutputCaptureExtension.class)
class AccessLogInterceptorTest {

    private final AccessLogInterceptor interceptor = new AccessLogInterceptor();

    @AfterEach
    void tearDown() {
        MDC.remove(MdcInterceptor.REQUEST_ID);
    }

    @Test
    void logsHttpCompletion(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/newsletter-issues");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/newsletter-issues");
        response.setStatus(HttpStatus.OK.value());
        MDC.put(MdcInterceptor.REQUEST_ID, "request-1");

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(output)
                .contains("HTTP completed.")
                .contains("method=GET")
                .contains("path=/api/v1/newsletter-issues")
                .contains("status=200")
                .contains("durationMs=")
                .contains("requestId=request-1");
    }
}
