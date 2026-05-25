package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsInboundEmailsResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsStatusSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin - Email Operations", description = "관리자 이메일 운영 콘솔 API")
public interface AdminEmailOperationsConsoleControllerApi {

    @Operation(
            summary = "인입 메일 상태 요약 조회",
            description = "관리자가 최근 24시간 인입 메일의 상태별 처리 건수를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    EmailOperationsStatusSummaryResponse getStatusSummary();

    @Operation(
            summary = "조치 필요 인입 메일 목록 조회",
            description = "관리자가 최근 24시간 인입 메일 중 운영 조치가 필요한 메일을 최신순으로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    EmailOperationsInboundEmailsResponse getActionRequiredItems(Pageable pageable);

    @Operation(
            summary = "처리 지연 인입 메일 목록 조회",
            description = "관리자가 10분 이상 RECEIVED 상태로 남은 인입 메일을 오래된 순으로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    EmailOperationsInboundEmailsResponse getStaleReceivedItems(Pageable pageable);
}
