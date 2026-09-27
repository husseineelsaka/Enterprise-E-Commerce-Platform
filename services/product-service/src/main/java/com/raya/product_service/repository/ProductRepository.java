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

    @org.springframework.data.jpa.repository.Query("SELECT p.id AS id, p.name AS name, p.price AS price, p.category AS categoryName FROM Product p WHERE p.id = :id")
    java.util.Optional<com.raya.product_service.projection.ProductSummaryProjection> findSummaryById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT p.id AS id, p.name AS name, p.price AS price, p.category AS categoryName FROM Product p")
    List<com.raya.product_service.projection.ProductSummaryProjection> findAllSummaries();
}

