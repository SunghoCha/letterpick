package com.sungho.letterpick.common.exception;

import com.sungho.letterpick.common.logging.MdcInterceptor;
import com.sungho.letterpick.member.domain.exception.DuplicateEmailException;
import com.sungho.letterpick.member.domain.exception.DuplicateNicknameException;
import com.sungho.letterpick.member.domain.exception.DuplicateSocialIdentityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        String requestId = MDC.get(MdcInterceptor.REQUEST_ID);

        if (errorCode.getStatus().is4xxClientError()) {
            log.info(
                    "비즈니스 거절. code={}, method={}, path={}, requestId={}, message={}",
                    errorCode.getCode(),
                    request.getMethod(),
                    request.getRequestURI(),
                    requestId,
                    exception.getMessage()
            );
        } else {
            log.warn(
                    "비즈니스 예외. code={}, method={}, path={}, requestId={}, message={}",
                    errorCode.getCode(),
                    request.getMethod(),
                    request.getRequestURI(),
                    requestId,
                    exception.getMessage()
            );
        }

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(exception));
    }

    /**
     * 권한 검증 실패는 SecurityConfig의 AccessDeniedHandler가 응답 포맷을 책임지도록 rethrow.
     * 여기서 잡으면 응답 형식이 두 곳으로 갈라진다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void propagateAccessDenied(AccessDeniedException exception) throws AccessDeniedException {
        throw exception;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = exception.getName() + ": 요청 값 형식이 올바르지 않습니다.";
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return ResponseEntity.status(CommonErrorCode.RESOURCE_NOT_FOUND.getStatus()).build();
        }

        log.info(
                "API 요청 경로를 찾을 수 없음. method={}, uri={}",
                request.getMethod(),
                uri
        );
        return ResponseEntity.status(CommonErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception
    ) {
        log.info(
                "허용되지 않은 HTTP 메서드. method={}, supportedMethods={}",
                exception.getMethod(),
                exception.getSupportedHttpMethods()
        );
        return ResponseEntity.status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    // TODO: 현재는 회원 도메인 unique constraint만 임시로 번역한다.
    //       다른 도메인의 DB constraint 번역이 필요해지면 MemberControllerAdvice 등 도메인별 advice로 분리한다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        BusinessException translated = translateUniqueConstraint(exception);
        if (translated != null) {
            return handleBusinessException(translated, request);
        }
        return handleUnexpected(exception, request);
    }

    private BusinessException translateUniqueConstraint(DataIntegrityViolationException exception) {
        String constraintName = findConstraintName(exception);
        if (constraintName == null) {
            return null;
        }

        String normalized = constraintName.toLowerCase(Locale.ROOT);
        if (normalized.contains("uk_member_email")) {
            return new DuplicateEmailException();
        }
        if (normalized.contains("uk_member_nickname")) {
            return new DuplicateNicknameException();
        }
        if (normalized.contains("uk_member_social_identity")) {
            return new DuplicateSocialIdentityException();
        }
        return null;
    }

    private String findConstraintName(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                return constraintViolation.getConstraintName();
            }
            current = current.getCause();
        }
        return exception.getMessage();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String requestId = MDC.get(MdcInterceptor.REQUEST_ID);
        log.error(
                "예상치 못한 예외. method={}, path={}, requestId={}",
                request.getMethod(),
                request.getRequestURI(),
                requestId,
                exception
        );
        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
