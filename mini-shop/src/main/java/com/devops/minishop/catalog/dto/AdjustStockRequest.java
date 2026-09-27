package com.devops.minishop.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(@NotNull Integer delta) {
}
