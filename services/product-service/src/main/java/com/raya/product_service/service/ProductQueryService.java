package com.raya.product_service.service;

import com.raya.product_service.projection.ProductSummaryDTO;
import com.raya.product_service.projection.ProductSummaryProjection;
import com.raya.product_service.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryService {
    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductSummaryDTO findSummaryByIdCached(Long id) {
        return productRepository.findSummaryById(id)
                .map(ProductSummaryDTO::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<ProductSummaryProjection> findById(Long id) {
        return Optional.ofNullable(findSummaryByIdCached(id));
    }

    @Cacheable(value = "products", key = "'all'")
    @Transactional(readOnly = true)
    public ArrayList<ProductSummaryDTO> findAllSummaries() {
        return new ArrayList<>(productRepository.findAllSummaries().stream()
                .map(ProductSummaryDTO::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryProjection> findAll() {
        return new ArrayList<>(findAllSummaries());
    }
}
