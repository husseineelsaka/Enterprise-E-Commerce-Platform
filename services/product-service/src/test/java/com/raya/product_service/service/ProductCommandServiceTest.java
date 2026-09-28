package com.raya.product_service.service;

import com.raya.product_service.event.ProductChangedEvent;
import com.raya.product_service.exception.InvalidProductException;
import com.raya.product_service.model.Product;
import com.raya.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductCommandService productCommandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_invalidPrice_throwsException() {
        Product invalidProduct = new Product("Bad", "Desc", BigDecimal.valueOf(-1), "ELECTRONICS");

        assertThrows(InvalidProductException.class, () -> productCommandService.create(invalidProduct));
        
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_validProduct_savesAndPublishesEvent() {
        Product validProduct = new Product("Laptop", "Desc", BigDecimal.valueOf(999.99), "ELECTRONICS");
        Product savedProduct = new Product(42L, "Laptop", "Desc", BigDecimal.valueOf(999.99), "ELECTRONICS");
        
        when(productRepository.save(validProduct)).thenReturn(savedProduct);

        Product result = productCommandService.create(validProduct);

        assertEquals(42L, result.getId());
        
        ArgumentCaptor<ProductChangedEvent> eventCaptor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        ProductChangedEvent event = eventCaptor.getValue();
        assertEquals(42L, event.productId());
        assertEquals("CREATED", event.changeType());
    }
}
