CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    value NUMERIC(19, 2) NOT NULL,
    min_order_amount NUMERIC(19, 2),
    max_discount_amount NUMERIC(19, 2),
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    per_user_limit INT DEFAULT 1,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE promotion_product_restrictions (
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    PRIMARY KEY (promotion_id, product_id)
);

CREATE TABLE promotion_category_restrictions (
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    category_id UUID NOT NULL,
    PRIMARY KEY (promotion_id, category_id)
);

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL UNIQUE,
    discount_amount NUMERIC(19, 2) NOT NULL,
    used_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_promotion_usages_promotion ON promotion_usages (promotion_id);
CREATE INDEX idx_promotion_usages_user ON promotion_usages (user_id);
CREATE INDEX idx_promotions_active_dates ON promotions (active, starts_at, ends_at);
