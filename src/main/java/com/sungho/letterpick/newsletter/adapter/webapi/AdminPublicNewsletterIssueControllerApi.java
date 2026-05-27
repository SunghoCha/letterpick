package com.sungho.letterpick.newsletter.adapter.webapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin - Public Newsletter Issue", description = "관리자 공개 뉴스레터 피드 관리 API")
public interface AdminPublicNewsletterIssueControllerApi {

    @Operation(
            summary = "공개 뉴스레터 피드 이슈 삭제",
            description = "관리자가 공개 피드 컬렉터 계정의 뉴스레터 이슈를 공개 피드에서 제거한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "404", description = "공개 피드 이슈를 찾을 수 없음")
    })
    void deleteIssue(Long issueId);
}
