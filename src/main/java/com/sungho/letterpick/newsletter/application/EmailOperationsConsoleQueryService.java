package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmailOperationsConsoleQueryService implements EmailOperationsConsoleFinder {

    private static final Duration STATUS_SUMMARY_WINDOW = Duration.ofHours(24);

    private final InboundEmailRepository inboundEmailRepository;
    private final Clock clock;

    @Override
    public InboundEmailStatusSummary findStatusSummary() {
        Instant receivedTo = Instant.now(clock);
        Instant receivedFrom = receivedTo.minus(STATUS_SUMMARY_WINDOW);
        List<InboundEmailStatusCount> statusCounts = fillMissingStatusCounts(
                inboundEmailRepository.countByStatus(receivedFrom, receivedTo)
        );
        long totalCount = statusCounts.stream()
                .mapToLong(InboundEmailStatusCount::count)
                .sum();

        return new InboundEmailStatusSummary(receivedFrom, receivedTo, totalCount, statusCounts);
    }

    private List<InboundEmailStatusCount> fillMissingStatusCounts(List<InboundEmailStatusCount> statusCounts) {
        EnumMap<InboundEmailStatus, Long> countByStatus = new EnumMap<>(InboundEmailStatus.class);
        Arrays.stream(InboundEmailStatus.values())
                .forEach(status -> countByStatus.put(status, 0L));
        statusCounts.forEach(statusCount -> countByStatus.put(statusCount.status(), statusCount.count()));

        return Arrays.stream(InboundEmailStatus.values())
                .map(status -> new InboundEmailStatusCount(status, countByStatus.get(status)))
                .toList();
    }
}
