package com.raya.product_service.service;

import com.raya.product_service.model.Product;
import com.raya.product_service.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "'all'")
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Cacheable(value = "products", key = "#id")
    public Product findByIdCached(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(findByIdCached(id));
    }

    @Caching(
            put = { @CachePut(value = "products", key = "#product.id") },
            evict = { @CacheEvict(value = "products", key = "'all'") }
    )
    public Product update(Product product) {
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "'all'")
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public boolean deleteById(Long id) {
        return productRepository.deleteById(id);
    }
}