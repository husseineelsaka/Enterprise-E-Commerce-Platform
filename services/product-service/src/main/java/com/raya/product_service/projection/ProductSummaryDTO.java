package com.raya.product_service.projection;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ProductSummaryDTO — Session 18 / CQRS.
 *
 * Concrete record implementing ProductSummaryProjection and Serializable.
 * This guarantees that Redis (using JdkSerializationRedisSerializer)
 * can cleanly cache product summaries without encountering
 * NotSerializableException from Spring Data's dynamic interface proxies.
 */
public record ProductSummaryDTO(
        Long id,
        String name,
        BigDecimal price,
        String categoryName
) implements ProductSummaryProjection, Serializable {

    public static ProductSummaryDTO from(ProductSummaryProjection p) {
        if (p == null) {
            return null;
        }
        if (p instanceof ProductSummaryDTO dto) {
            return dto;
        }
        return new ProductSummaryDTO(p.getId(), p.getName(), p.getPrice(), p.getCategoryName());
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String getCategoryName() {
        return categoryName;
    }
}
