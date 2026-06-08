package com.sungho.letterpick.trending.application;

import com.sungho.letterpick.event.EventEnvelope;
import com.sungho.letterpick.trending.inbox.InboxEvent;
import com.sungho.letterpick.trending.inbox.InboxEventRepository;
import com.sungho.letterpick.trending.inbox.InboxEventStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
public class TransactionalTrendingEventProcessingService implements TrendingEventProcessingService {

    private final InboxEventRepository inboxEventRepository;
    private final Map<String, TrendingEventHandler> handlers;
    private final Clock clock;

    public TransactionalTrendingEventProcessingService(InboxEventRepository inboxEventRepository,
                                                       List<TrendingEventHandler> handlers,
                                                       Clock clock) {
        this.inboxEventRepository = requireNonNull(inboxEventRepository, "inboxEventRepository must not be null");
        requireNonNull(handlers, "handlers must not be null");
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TrendingEventHandler::eventType,
                        Function.identity()
                ));
        this.clock = requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void process(EventEnvelope<JsonNode> envelope) {
        InboxEvent inboxEvent = inboxEventRepository.findByEventIdForUpdate(envelope.eventId())
                .orElseThrow(() -> new TrendingMessageProcessingException(
                        "inbox event is not recorded: " + envelope.eventId()));
        if (inboxEvent.getStatus() == InboxEventStatus.PROCESSED) {
            return;
        }

        dispatch(envelope);
        inboxEvent.markProcessed(clock.instant());
    }

    private void dispatch(EventEnvelope<JsonNode> envelope) {
        TrendingEventHandler handler = handlers.get(envelope.eventType());
        if (handler == null) {
            throw new TrendingMessageProcessingException("unsupported trending event type: " + envelope.eventType());
        }
        handler.handle(envelope);
    }
}
