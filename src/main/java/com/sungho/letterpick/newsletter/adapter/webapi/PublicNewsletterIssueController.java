package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.PublicNewsletterIssueDetailResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.PublicNewsletterIssuesResponse;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueDetail;
import com.sungho.letterpick.newsletter.application.provided.NewsletterIssueItem;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueFinder;
import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueSearchCondition;
import com.sungho.letterpick.newsletter.domain.NewsletterCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/newsletter-issues")
@RequiredArgsConstructor
public class PublicNewsletterIssueController implements PublicNewsletterIssueControllerApi {

    private final PublicNewsletterIssueFinder publicNewsletterIssueFinder;

    @Override
    @GetMapping
    public PublicNewsletterIssuesResponse getIssues(
            @RequestParam(required = false) NewsletterCategory category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Slice<NewsletterIssueItem> issueItems = publicNewsletterIssueFinder.findIssues(
                new PublicNewsletterIssueSearchCondition(category, keyword),
                pageable
        );
        return PublicNewsletterIssuesResponse.from(issueItems);
    }

    @Override
    @GetMapping("/{issueId}")
    public PublicNewsletterIssueDetailResponse getIssueDetail(@PathVariable("issueId") Long issueId) {
        NewsletterIssueDetail issueDetail = publicNewsletterIssueFinder.findIssueDetail(issueId);
        return PublicNewsletterIssueDetailResponse.from(issueDetail);
    }
}
