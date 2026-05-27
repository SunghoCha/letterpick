package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.application.provided.PublicNewsletterIssueModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/public-newsletter-issues")
@RequiredArgsConstructor
public class AdminPublicNewsletterIssueController implements AdminPublicNewsletterIssueControllerApi {

    private final PublicNewsletterIssueModifier publicNewsletterIssueModifier;

    @Override
    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable("issueId") Long issueId) {
        publicNewsletterIssueModifier.delete(issueId);
    }
}
