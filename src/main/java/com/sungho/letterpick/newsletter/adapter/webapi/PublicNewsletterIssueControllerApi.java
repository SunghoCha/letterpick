package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.PublicNewsletterIssueDetailResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.PublicNewsletterIssuesResponse;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "Public Newsletter Issue", description = "공개 뉴스레터 이슈 API")
public interface PublicNewsletterIssueControllerApi {

    @Operation(
            summary = "공개 뉴스레터 피드 목록 조회",
            description = "비로그인 사용자를 포함해 공개 뉴스레터 피드 목록을 조회한다. category query parameter가 있으면 해당 카테고리로 필터링하고, keyword query parameter가 있으면 제목과 본문을 검색한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 query parameter 형식 오류")
    })
    PublicNewsletterIssuesResponse getIssues(NewsletterCategory category,
                                             String keyword,
                                             Pageable pageable);

    @Operation(
            summary = "공개 뉴스레터 이슈 상세 조회",
            description = "비로그인 사용자를 포함해 공개 뉴스레터 피드에서 선택한 이슈의 상세 본문을 조회한다. 개인 보관함의 읽음 상태는 변경하지 않는다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "이슈 없음")
    })
    PublicNewsletterIssueDetailResponse getIssueDetail(Long issueId);

    @Operation(
            summary = "공개 뉴스레터 이슈 조회수 기록",
            description = "공개 뉴스레터 이슈 상세 화면 표시 후 조회수를 기록한다. 로그인 사용자는 회원 기준, 비로그인 사용자는 anonymousId 쿠키 기준으로 중복 조회를 방지한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "조회수 기록 요청 처리 완료")
    })
    void recordIssueView(Long issueId,
                         @Parameter(hidden = true) PublicIssueViewActor actor);
}
