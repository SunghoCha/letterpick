package com.sungho.letterpick.newsletter.application;

import com.sungho.letterpick.newsletter.adapter.persistence.InboundEmailRepository;
import com.sungho.letterpick.newsletter.application.provided.EmailOperationsConsoleFinder;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailAdminItem;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusCount;
import com.sungho.letterpick.newsletter.application.provided.InboundEmailStatusSummary;
import com.sungho.letterpick.newsletter.domain.InboundEmailStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmailOperationsConsoleQueryService implements EmailOperationsConsoleFinder {

    private static final Duration RECENT_WINDOW = Duration.ofHours(24);
    private static final Duration STALE_RECEIVED_THRESHOLD = Duration.ofMinutes(10);

    private final InboundEmailRepository inboundEmailRepository;
    private final Clock clock;

    @Override
    public InboundEmailStatusSummary findStatusSummary() {
        Instant receivedTo = Instant.now(clock);
        Instant receivedFrom = receivedTo.minus(RECENT_WINDOW);
        List<InboundEmailStatusCount> statusCounts = fillMissingStatusCounts(
                inboundEmailRepository.countByStatus(receivedFrom, receivedTo)
        );
        long totalCount = statusCounts.stream()
                .mapToLong(InboundEmailStatusCount::count)
                .sum();

        return new InboundEmailStatusSummary(receivedFrom, receivedTo, totalCount, statusCounts);
    }

    @Override
    public Slice<InboundEmailAdminItem> findActionRequiredItems(Pageable pageable) {
        Instant receivedTo = Instant.now(clock);
        Instant receivedFrom = receivedTo.minus(RECENT_WINDOW);

        return inboundEmailRepository.findActionRequired(receivedFrom, receivedTo, pageable);
    }

    @Override
    public Slice<InboundEmailAdminItem> findStaleReceivedItems(Pageable pageable) {
        Instant receivedBefore = Instant.now(clock).minus(STALE_RECEIVED_THRESHOLD);

        return inboundEmailRepository.findStaleReceived(receivedBefore, pageable);
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
