package com.sungho.letterpick.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ApiControllerAdviceTest {

    private final ApiControllerAdvice advice = new ApiControllerAdvice();

    @Test
    void returnsEmptyNotFoundForNonApiNoResource() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/", "classpath:/static/");

        ResponseEntity<?> response = advice.handleNoResource(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void returnsNotFoundBodyForApiNoResource() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unknown");
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/unknown", "classpath:/static/");

        ResponseEntity<?> response = advice.handleNoResource(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.code()).isEqualTo("CMN-001");
        assertThat(body.message()).isEqualTo("요청한 리소스를 찾을 수 없습니다");
    }

    @Test
    void returnsMethodNotAllowedForUnsupportedMethod() {
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ResponseEntity<ErrorResponse> response = advice.handleMethodNotAllowed(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CMN-002");
        assertThat(response.getBody().message()).isEqualTo("허용되지 않은 HTTP 메서드입니다");
    }
}
