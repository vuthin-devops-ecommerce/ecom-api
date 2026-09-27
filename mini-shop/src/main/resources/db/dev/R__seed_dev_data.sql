-- Dev-only seed (applied only when the 'dev' profile adds classpath:db/dev to Flyway locations).
-- Repeatable migration: re-runs whenever this file's checksum changes; ON CONFLICT keeps it idempotent.

INSERT INTO products (sku, name, price, stock) VALUES
    ('SKU-001', 'Widget',            9.99,  50),
    ('SKU-002', 'Gadget',           19.50,  30),
    ('SKU-003', 'USB-C Cable 1m',    4.25, 200),
    ('SKU-004', 'Mechanical Keyboard', 89.00, 12),
    ('SKU-005', 'Wireless Mouse',   24.90,  40),
    ('SKU-006', '27" Monitor',     249.00,   5),
    ('SKU-007', 'Laptop Stand',     32.00,   0),
    ('SKU-008', 'Webcam 1080p',     45.00,   8)
ON CONFLICT (sku) DO NOTHING;
