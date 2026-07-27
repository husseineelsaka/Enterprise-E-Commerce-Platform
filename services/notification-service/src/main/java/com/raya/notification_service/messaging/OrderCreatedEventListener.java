package com.raya.notification_service.messaging;

import com.raya.notification_service.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);


    @KafkaListener(
            topics = "order-created",
            groupId = "notification-service"
    )
    public void handle(OrderCreatedEvent event) {

        log.info(
                """ 
                ======================================== 
                NOTIFICATION SERVICE 
                Sending order confirmation email... 
                Order ID: %s
                Customer ID: %s
                Total Amount: %s
                ========================================
                """.formatted(
                        event.orderId(),
                        event.customerId(),
                        event.amount()
                )
        );
    }
}