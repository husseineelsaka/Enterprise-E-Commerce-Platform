package com.raya.order_service.service;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.messaging.OrderEventPublisher;
import com.raya.order_service.model.Order;
import com.raya.order_service.model.OrderStatus;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.OrderSagaEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.raya.order_service.repository.OutboxRepository outboxRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    // OrderSagaEventHandler publishes OrderConfirmedEvent on the happy path.
    // Without a mock declared here @InjectMocks leaves the field null and the
    // confirmation test NPEs.
    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private OrderSagaEventHandler sagaEventHandler;

    private OrderRequest sampleRequest() {
        return new OrderRequest("PROD-001", 3, new BigDecimal("100.00"), "CUST-1");
    }

    // ── OrderService.createOrder() ───────────────────────────────────────

    @Test
    void createOrder_returnsPendingAndPersistsOrder() {
        OrderResponse response = orderService.createOrder(sampleRequest());

        assertThat(response.orderId()).isNotBlank();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.message()).contains("processing");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_publishesOrderPlacedEvent() throws Exception {
        org.mockito.Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"test\"}");

        OrderResponse response = orderService.createOrder(sampleRequest());

        ArgumentCaptor<com.raya.order_service.model.OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(com.raya.order_service.model.OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        var event = outboxCaptor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getEventType()).isEqualTo("OrderPlacedEvent");
        assertThat(event.getAggregateId()).isEqualTo(response.orderId());
        assertThat(event.getPayload()).isEqualTo("{\"payload\":\"test\"}");
        assertThat(event.isPublished()).isFalse();
    }

    // ── OrderSagaEventHandler (final saga outcomes) ──────────────────────

    @Test
    void handlePaymentEvent_paymentCompleted_marksOrderConfirmed() {
        Order existing = new Order("ORD-1", "PROD-001", 3, new BigDecimal("100"), OrderStatus.PENDING, "CUST-1");
        org.mockito.Mockito.when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(existing));

        sagaEventHandler.handlePaymentEvent("{\"type\":\"PaymentCompletedEvent\",\"orderId\":\"ORD-1\",\"transactionId\":\"TX-9\"}", "payment-events");

        assertThat(existing.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(existing);
    }

    @Test
    void handlePaymentEvent_paymentFailed_marksOrderPaymentFailed() {
        Order existing = new Order("ORD-2", "PROD-001", 3, new BigDecimal("100"), OrderStatus.PENDING, "CUST-1");
        org.mockito.Mockito.when(orderRepository.findById("ORD-2")).thenReturn(Optional.of(existing));

        sagaEventHandler.handlePaymentEvent("{\"type\":\"PaymentFailedEvent\",\"orderId\":\"ORD-2\",\"reason\":\"declined\"}", "payment-events");

        assertThat(existing.status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository).save(existing);
    }

    @Test
    void handleInventoryReleased_marksOrderCancelled() {
        Order existing = new Order("ORD-3", "PROD-001", 3, new BigDecimal("100"), OrderStatus.PAYMENT_FAILED, "CUST-1");
        org.mockito.Mockito.when(orderRepository.findById("ORD-3")).thenReturn(Optional.of(existing));

        sagaEventHandler.handleInventoryReleased("{\"type\":\"InventoryReleasedEvent\",\"orderId\":\"ORD-3\"}");

        assertThat(existing.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(existing);
    }
}
