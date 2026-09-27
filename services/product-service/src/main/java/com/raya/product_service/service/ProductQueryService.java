package com.raya.product_service.service;

import com.raya.product_service.projection.ProductSummaryProjection;
import com.raya.product_service.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryService {
    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @org.springframework.cache.annotation.Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<ProductSummaryProjection> findById(Long id) {
        return productRepository.findSummaryById(id);
    }

    @org.springframework.cache.annotation.Cacheable(value = "products", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductSummaryProjection> findAll() {
        return productRepository.findAllSummaries();
    }
}
