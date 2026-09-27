package com.devops.minishop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Plain id, not @ManyToOne Product: keeps 'order' independent of 'catalog' entities (ADR-003).
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_order", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtOrder;

    public OrderItem(Long productId, Integer quantity, BigDecimal priceAtOrder) {
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    public BigDecimal lineTotal() {
        return priceAtOrder.multiply(BigDecimal.valueOf(quantity));
    }
}
