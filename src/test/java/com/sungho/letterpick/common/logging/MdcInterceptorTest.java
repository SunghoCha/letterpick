package com.sungho.letterpick.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcInterceptorTest {

    private final MdcInterceptor interceptor = new MdcInterceptor();

    @AfterEach
    void tearDown() {
        MDC.remove(MdcInterceptor.REQUEST_ID);
    }

    @Test
    void usesHeaderRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/newsletter-issues");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(MdcInterceptor.REQUEST_ID_HEADER, "test-request-id");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader(MdcInterceptor.REQUEST_ID_HEADER)).isEqualTo("test-request-id");
        assertThat(MDC.get(MdcInterceptor.REQUEST_ID)).isEqualTo("test-request-id");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(MdcInterceptor.REQUEST_ID)).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/newsletter-issues");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader(MdcInterceptor.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get(MdcInterceptor.REQUEST_ID))
                .isEqualTo(response.getHeader(MdcInterceptor.REQUEST_ID_HEADER));
    }
}
