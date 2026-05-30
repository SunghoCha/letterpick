package com.sungho.letterpick.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class MdcInterceptor implements AsyncHandlerInterceptor {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final String REQUEST_ID_ATTRIBUTE = MdcInterceptor.class.getName() + ".requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = resolveRequestId(request);

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID, requestId);

        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MDC.remove(REQUEST_ID);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        MDC.remove(REQUEST_ID);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object existingRequestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existingRequestId instanceof String requestId && StringUtils.hasText(requestId)) {
            return requestId;
        }

        String headerRequestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        if (StringUtils.hasText(headerRequestId) && headerRequestId.length() <= MAX_REQUEST_ID_LENGTH) {
            return headerRequestId;
        }

        return UUID.randomUUID().toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim()
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('\t', '_');
    }
}
