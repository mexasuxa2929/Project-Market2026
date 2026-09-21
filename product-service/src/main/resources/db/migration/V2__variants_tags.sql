CREATE TABLE IF NOT EXISTS product_tag (
    id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product_to_tag (
    product_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    CONSTRAINT fk_product_to_tag_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_to_tag_tag FOREIGN KEY (tag_id) REFERENCES product_tag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_variant (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    attributes TEXT NOT NULL,
    price_modifier NUMERIC(19,2) NOT NULL DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(512),
    CONSTRAINT fk_product_variant_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_variant_product_id ON product_variant(product_id);
CREATE INDEX IF NOT EXISTS idx_product_tag_name ON product_tag(name);
