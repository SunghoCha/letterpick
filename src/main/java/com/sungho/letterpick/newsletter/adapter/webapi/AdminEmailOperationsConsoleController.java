package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsInboundEmailsResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsQueueStatusResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsStatusSummaryResponse;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/email-operations")
@RequiredArgsConstructor
public class AdminEmailOperationsConsoleController implements AdminEmailOperationsConsoleControllerApi {

    private final EmailOperationsConsoleFinder emailOperationsConsoleFinder;

    @Override
    @GetMapping("/status-summary")
    public EmailOperationsStatusSummaryResponse getStatusSummary() {
        return EmailOperationsStatusSummaryResponse.from(emailOperationsConsoleFinder.findStatusSummary());
    }

    @Override
    @GetMapping("/action-required")
    public EmailOperationsInboundEmailsResponse getActionRequiredItems(@PageableDefault(size = 20) Pageable pageable) {
        return EmailOperationsInboundEmailsResponse.from(
                emailOperationsConsoleFinder.findActionRequiredItems(pageable)
        );
    }

    @Override
    @GetMapping("/stale-received")
    public EmailOperationsInboundEmailsResponse getStaleReceivedItems(@PageableDefault(size = 20) Pageable pageable) {
        return EmailOperationsInboundEmailsResponse.from(
                emailOperationsConsoleFinder.findStaleReceivedItems(pageable)
        );
    }

    @Override
    @GetMapping("/queue-status")
    public EmailOperationsQueueStatusResponse getQueueStatus() {
        return EmailOperationsQueueStatusResponse.from(
                emailOperationsConsoleFinder.findQueueStatus()
        );
    }
}
