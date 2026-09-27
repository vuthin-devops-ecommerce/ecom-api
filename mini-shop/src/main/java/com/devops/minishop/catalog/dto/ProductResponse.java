package com.devops.minishop.catalog.dto;

import com.devops.minishop.catalog.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        Integer stock,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(), p.getCreatedAt());
    }
}
