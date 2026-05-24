package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsStatusSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
}
