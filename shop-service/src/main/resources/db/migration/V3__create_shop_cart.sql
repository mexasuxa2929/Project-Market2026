-- Normalized cart: one shop_cart per user, line items in shop_cart_item.
-- Replaces legacy flat shop_cart_item from V1/V2.

DROP TABLE IF EXISTS shop_cart_item CASCADE;
DROP TABLE IF EXISTS shop_cart CASCADE;

CREATE TABLE shop_cart (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL UNIQUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE shop_cart_item (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id     UUID        NOT NULL REFERENCES shop_cart(id) ON DELETE CASCADE,
    product_id  UUID        NOT NULL,
    quantity    INT         NOT NULL DEFAULT 1,
    price       NUMERIC(19, 2),
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_shop_cart_item_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_item_cart_id ON shop_cart_item (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_product_id ON shop_cart_item (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_user_id ON shop_cart (user_id);
