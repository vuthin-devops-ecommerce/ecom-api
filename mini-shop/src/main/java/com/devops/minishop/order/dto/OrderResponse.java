package com.devops.minishop.order.dto;

import com.devops.minishop.order.Order;
import com.devops.minishop.order.OrderItem;
import com.devops.minishop.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<ItemResponse> items
) {
    public record ItemResponse(Long productId, Integer quantity, BigDecimal priceAtOrder, BigDecimal lineTotal) {

        static ItemResponse from(OrderItem i) {
            return new ItemResponse(i.getProductId(), i.getQuantity(), i.getPriceAtOrder(), i.lineTotal());
        }
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getCreatedAt(),
                o.getItems().stream().map(ItemResponse::from).toList());
    }
}
