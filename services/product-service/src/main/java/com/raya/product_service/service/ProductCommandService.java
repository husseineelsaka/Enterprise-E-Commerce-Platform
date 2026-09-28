package com.raya.product_service.service;

import com.raya.product_service.event.ProductChangedEvent;
import com.raya.product_service.exception.InvalidProductException;
import com.raya.product_service.model.Product;
import com.raya.product_service.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductCommandService {
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductCommandService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product create(Product product) {
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Price must be positive");
        }
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductChangedEvent(saved.getId(), "CREATED"));
        return saved;
    }

    @Transactional
    public Product update(Product product) {
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Price must be positive");
        }
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductChangedEvent(saved.getId(), "UPDATED"));
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        productRepository.deleteById(id);
        eventPublisher.publishEvent(new ProductChangedEvent(id, "DELETED"));
    }
}
