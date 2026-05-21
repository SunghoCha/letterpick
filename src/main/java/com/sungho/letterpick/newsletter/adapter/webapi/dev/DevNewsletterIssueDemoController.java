package com.sungho.letterpick.newsletter.adapter.webapi.dev;

import com.sungho.letterpick.common.auth.CurrentUser;
import com.sungho.letterpick.common.auth.LoginUser;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoResult;
import com.sungho.letterpick.newsletter.application.dev.DevNewsletterIssueDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me/newsletter-issues/demo")
public class DevNewsletterIssueDemoController {

    private final DevNewsletterIssueDemoService demoService;

    @PostMapping
    public DevNewsletterIssueDemoResponse createDemoIssues(@CurrentUser LoginUser loginUser) {
        DevNewsletterIssueDemoResult result = demoService.createFor(loginUser.memberId());
        return DevNewsletterIssueDemoResponse.from(result);
    }
}
