package com.raya.product_service.repository;

import com.raya.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Derived query — exercised against a real PostgreSQL in ProductRepositoryIntegrationTest. */
    List<Product> findByPriceLessThan(BigDecimal price);
}

