package com.raya.product_service.model;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record Product(
        Long id,
        @NotBlank @Size(max = 50)
        String name,
        @NotBlank @Size(max = 250)
        String description,
        @NotNull  @Positive
        BigDecimal price,
        @NotBlank
        String category
) {}