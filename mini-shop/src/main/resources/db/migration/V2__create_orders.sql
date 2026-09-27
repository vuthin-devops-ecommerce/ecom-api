-- Design rationale: docs/decisions/002-order-schema-fk.md

CREATE TABLE orders (
    id            BIGSERIAL     PRIMARY KEY,
    status        VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    total_amount  NUMERIC(12,2) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_orders_status       CHECK (status IN ('CREATED', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0)
);

CREATE TABLE order_items (
    id              BIGSERIAL     PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    quantity        INTEGER       NOT NULL,
    price_at_order  NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_order_items_order         FOREIGN KEY (order_id)   REFERENCES orders (id)   ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product       FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_quantity     CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price        CHECK (price_at_order >= 0),
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id)
);

-- PostgreSQL does not index FK columns automatically; RESTRICT checks and "orders containing product X" need this.
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
