package com.sungho.letterpick.newsletter.adapter.webapi;

import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsInboundEmailsResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsQueueStatusResponse;
import com.sungho.letterpick.newsletter.adapter.webapi.dto.EmailOperationsStatusSummaryResponse;
import com.sungho.letterpick.newsletter.application.exception.InvalidEmailOperationsSearchConditionException;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsSearchCondition;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/email-operations")
@RequiredArgsConstructor
public class AdminEmailOperationsConsoleController implements AdminEmailOperationsConsoleControllerApi {

    private final EmailOperationsConsoleFinder emailOperationsConsoleFinder;

    @Override
    @GetMapping("/status-summary")
    public EmailOperationsStatusSummaryResponse getStatusSummary(
            @RequestParam(required = false) String receivedFrom,
            @RequestParam(required = false) String receivedTo
    ) {
        EmailOperationsSearchCondition searchCondition = parseSearchCondition(receivedFrom, receivedTo);

        return EmailOperationsStatusSummaryResponse.from(
                emailOperationsConsoleFinder.findStatusSummary(searchCondition)
        );
    }

    @Override
    @GetMapping("/action-required")
    public EmailOperationsInboundEmailsResponse getActionRequiredItems(
            @RequestParam(required = false) String receivedFrom,
            @RequestParam(required = false) String receivedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        EmailOperationsSearchCondition searchCondition = parseSearchCondition(receivedFrom, receivedTo);

        return EmailOperationsInboundEmailsResponse.from(
                emailOperationsConsoleFinder.findActionRequiredItems(searchCondition, pageable)
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

    private EmailOperationsSearchCondition parseSearchCondition(String receivedFrom, String receivedTo) {
        if (receivedFrom == null && receivedTo == null) {
            return EmailOperationsSearchCondition.empty();
        }
        if (receivedFrom == null || receivedTo == null) {
            throw new InvalidEmailOperationsSearchConditionException();
        }

        try {
            return EmailOperationsSearchCondition.receivedAtRange(
                    Instant.parse(receivedFrom),
                    Instant.parse(receivedTo)
            );
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidEmailOperationsSearchConditionException();
        }
    }
}
