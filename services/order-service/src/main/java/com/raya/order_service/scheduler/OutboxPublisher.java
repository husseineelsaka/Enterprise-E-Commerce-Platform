package com.raya.order_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.order_service.model.OutboxEvent;
import com.raya.order_service.repository.OutboxRepository;
import com.raya.order_service.saga.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        var pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                if ("OrderPlacedEvent".equals(event.getEventType())) {
                    OrderPlacedEvent payloadObject = objectMapper.readValue(event.getPayload(), OrderPlacedEvent.class);
                    kafkaTemplate.send("order-events", event.getAggregateId(), payloadObject);
                    event.setPublished(true);
                    outboxRepository.save(event);
                    log.info("[OUTBOX] Successfully published OrderPlacedEvent for order: {}", event.getAggregateId());
                }
            } catch (Exception e) {
                log.error("[OUTBOX] Failed to publish event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }
}
