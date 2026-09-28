package com.raya.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * NotificationService — Session 13
 * Full implementation with @RetryableTopic (3 attempts, exponential backoff)
 * and Dead Letter Topic (@DltHandler).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // ── PAYMENT COMPLETED: send order confirmation email ─────────────
    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentCompleted(String rawEvent) {
        if (!rawEvent.contains("PaymentCompleted")) return;
        log.info("[NOTIFICATION] Sending confirmation email for event: {}", rawEvent);
        sendConfirmationEmail(rawEvent);
        log.info("[NOTIFICATION] ✅ Confirmation sent");
    }

    // ── ORDER CANCELLED: send cancellation notification ──────────────
    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "inventory-events", groupId = "notification-service-cancel")
    public void handleOrderCancelled(String rawEvent) {
        if (!rawEvent.contains("OrderCancelled")) return;
        log.info("[NOTIFICATION] Sending cancellation notification for event: {}", rawEvent);
        sendCancellationNotification(rawEvent);
    }

    // ── DLT HANDLER: log events that exhausted all retries ───────────
    @DltHandler
    public void handleDlt(String rawEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[NOTIFICATION] ✅ DLT: Event from topic '{}' exhausted all retries. " +
                "Manual intervention required. Event: {}", topic, rawEvent);
    }

    private void sendConfirmationEmail(String details) {
        log.info("[EMAIL] Order confirmed: {}", details);
    }

    private void sendCancellationNotification(String details) {
        log.info("[EMAIL] Order cancelled: {}", details);
    }
}
