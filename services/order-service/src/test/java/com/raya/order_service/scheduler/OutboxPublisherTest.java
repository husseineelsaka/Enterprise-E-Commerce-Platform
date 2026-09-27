package com.raya.order_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.order_service.model.OutboxEvent;
import com.raya.order_service.repository.OutboxRepository;
import com.raya.order_service.saga.event.OrderPlacedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    void shouldPublishPendingEvents() throws Exception {
        OutboxEvent event = new OutboxEvent("Order", "123", "OrderPlacedEvent", "{}");
        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        
        OrderPlacedEvent mockEvent = new OrderPlacedEvent("123", "P1", 1, new java.math.BigDecimal("100.0"), "C1");
        when(objectMapper.readValue("{}", OrderPlacedEvent.class)).thenReturn(mockEvent);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(eq("order-events"), eq("123"), eq(mockEvent));
        assertTrue(event.isPublished());
        verify(outboxRepository).save(event);
    }

    @Test
    void shouldHandleExceptionAndContinue() throws Exception {
        OutboxEvent event = new OutboxEvent("Order", "123", "OrderPlacedEvent", "{}");
        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        
        when(objectMapper.readValue("{}", OrderPlacedEvent.class)).thenThrow(new RuntimeException("JSON Parse Error"));

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxRepository, never()).save(event);
    }
}
