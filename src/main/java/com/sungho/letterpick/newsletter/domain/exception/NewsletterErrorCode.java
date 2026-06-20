package com.sungho.letterpick.newsletter.domain.exception;

import com.sungho.letterpick.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NewsletterErrorCode implements ErrorCode {

    NEWSLETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "NWL-001", "뉴스레터를 찾을 수 없습니다"),
    MEMBER_NEWSLETTER_NOT_FOUND(HttpStatus.NOT_FOUND, "NWL-002", "회원의 뉴스레터 구독 정보를 찾을 수 없습니다"),
    NEWSLETTER_ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "NWL-003", "뉴스레터 이슈를 찾을 수 없습니다"),
    PUBLIC_ISSUE_RANKING_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "NWL-004", "인기 이슈 랭킹을 조회할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
