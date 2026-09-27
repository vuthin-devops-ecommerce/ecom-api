CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)   NOT NULL UNIQUE,
    name        VARCHAR(200)  NOT NULL,
    price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock       INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
