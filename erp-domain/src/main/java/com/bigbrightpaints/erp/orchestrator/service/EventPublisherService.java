package com.bigbrightpaints.erp.orchestrator.service;

import com.bigbrightpaints.erp.orchestrator.event.DomainEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RETRY_BASE_DELAY_SECONDS = 30;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisherService(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(event.entity(), event.entityId(), event.eventType(), payload);
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }

    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository
                .findTop10ByStatusAndDeadLetterFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING, Instant.now());
        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend("bbp.orchestrator.events", event.getEventType(), event.getPayload());
                event.markPublished();
            } catch (Exception ex) {
                log.error("Failed to publish event {}", event.getId(), ex);
                long delaySeconds = computeBackoffDelay(event.getRetryCount());
                event.scheduleRetry(ex.getMessage(), MAX_RETRY_ATTEMPTS, delaySeconds);
            }
        }
    }

    private long computeBackoffDelay(int retryCount) {
        int exponent = Math.min(retryCount, 10);
        return (long) Math.pow(2, exponent) * RETRY_BASE_DELAY_SECONDS;
    }

    public Map<String, Object> healthSnapshot() {
        Map<String, Object> health = new HashMap<>();
        health.put("pendingEvents", outboxEventRepository.count());
        health.put("deadLetters", outboxEventRepository.countByStatusAndDeadLetterTrue(OutboxEvent.Status.FAILED));
        return health;
    }
}
