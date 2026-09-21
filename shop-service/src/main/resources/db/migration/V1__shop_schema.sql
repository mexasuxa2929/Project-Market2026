CREATE TABLE IF NOT EXISTS shop_cart_item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1 AND quantity <= 9999),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shop_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_shop_cart_user ON shop_cart_item (user_id);

CREATE TABLE IF NOT EXISTS shop_wishlist_item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shop_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_shop_wishlist_user ON shop_wishlist_item (user_id);

CREATE TABLE IF NOT EXISTS shop_customer_address (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    label VARCHAR(128),
    region VARCHAR(128),
    district VARCHAR(128),
    line1 VARCHAR(512) NOT NULL,
    line2 VARCHAR(512),
    postal_code VARCHAR(32),
    phone VARCHAR(64),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_address_user ON shop_customer_address (user_id);

CREATE TABLE IF NOT EXISTS shop_product_review (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shop_review_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_shop_review_product ON shop_product_review (product_id);
