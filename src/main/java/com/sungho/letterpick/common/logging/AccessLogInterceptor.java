package com.sungho.letterpick.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = AccessLogInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute(START_TIME_ATTRIBUTE) == null) {
            request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        long durationMs = calculateDurationMs(request);
        String path = resolvePath(request);
        int status = response.getStatus();
        String requestId = MDC.get(MdcInterceptor.REQUEST_ID);

        if (status >= 500) {
            log.warn(
                    "HTTP completed with server error. method={}, path={}, status={}, durationMs={}, requestId={}",
                    request.getMethod(),
                    path,
                    status,
                    durationMs,
                    requestId
            );
            return;
        }

        if (status >= 400) {
            log.info(
                    "HTTP rejected. method={}, path={}, status={}, durationMs={}, requestId={}",
                    request.getMethod(),
                    path,
                    status,
                    durationMs,
                    requestId
            );
            return;
        }

        log.info(
                "HTTP completed. method={}, path={}, status={}, durationMs={}, requestId={}",
                request.getMethod(),
                path,
                status,
                durationMs,
                requestId
        );
    }

    private long calculateDurationMs(HttpServletRequest request) {
        Object startTimeValue = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTimeValue instanceof Long startTime) {
            return (System.nanoTime() - startTime) / 1_000_000L;
        }
        return 0L;
    }

    private String resolvePath(HttpServletRequest request) {
        Object patternValue = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (patternValue instanceof String pattern) {
            return pattern;
        }
        return request.getRequestURI();
    }
}
