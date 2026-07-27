package com.raya.notification_service.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.notification_service.event.OrderConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedEventListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "order-notification",
            groupId = "notification-service-confirmed"
    )
    public void handle(String rawEvent) {

        try {
            JsonNode node = objectMapper.readTree(rawEvent);
            if (!"OrderConfirmedEvent".equals(node.path("type").asText())) {
                log.debug("Ignoring non-confirmed event on order-confirmed topic: {}", node.path("type").asText());
                return;
            }
            OrderConfirmedEvent event = objectMapper.treeToValue(node, OrderConfirmedEvent.class);

            log.info(
                    """
                    
                    ========================================
                    NOTIFICATION SERVICE
                    Sending order confirmation email...
                    Order ID: %s
                    Customer ID: %s
                    Total Amount: %s
                    Transaction ID: %s
                    ========================================
                    """.formatted(
                            event.orderId(),
                            event.customerId(),
                            event.amount(),
                            event.transactionId()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to process order-confirmed event: {}", rawEvent, e);
        }
    }
}
