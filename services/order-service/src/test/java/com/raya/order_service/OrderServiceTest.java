package com.raya.order_service.service;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
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
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

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
    void createOrder_publishesOrderPlacedEvent() {
        OrderResponse response = orderService.createOrder(sampleRequest());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-events");
        assertThat(eventCaptor.getValue()).isInstanceOf(com.raya.order_service.saga.event.OrderPlacedEvent.class);
        var event = (com.raya.order_service.saga.event.OrderPlacedEvent) eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(response.orderId());
        assertThat(event.productId()).isEqualTo("PROD-001");
        assertThat(event.quantity()).isEqualTo(3);
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
