CREATE TABLE IF NOT EXISTS product_price_tier (
    id          UUID           PRIMARY KEY,
    product_id  UUID           NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    min_qty     INTEGER        NOT NULL CHECK (min_qty >= 0),
    max_qty     INTEGER        CHECK (max_qty IS NULL OR max_qty >= min_qty),
    price       NUMERIC(38, 2) NOT NULL CHECK (price >= 0),
    currency    VARCHAR(10)    NOT NULL DEFAULT 'UZS',
    price_type  VARCHAR(20)    NOT NULL DEFAULT 'RETAIL',
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_price_tier_product_id ON product_price_tier(product_id);
CREATE INDEX IF NOT EXISTS idx_price_tier_product_minqty ON product_price_tier(product_id, min_qty);
