ALTER TABLE product
    ADD COLUMN IF NOT EXISTS featured        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS digital         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS description     TEXT,
    ADD COLUMN IF NOT EXISTS short_description VARCHAR(300),
    ADD COLUMN IF NOT EXISTS sku             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS slug            VARCHAR(200),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(160),
    ADD COLUMN IF NOT EXISTS material        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country_of_origin VARCHAR(100),
    ADD COLUMN IF NOT EXISTS warranty_months INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by      UUID,
    ADD COLUMN IF NOT EXISTS created_at      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS idx_product_slug ON product(slug) WHERE slug IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_product_status   ON product(status);
CREATE INDEX IF NOT EXISTS idx_product_featured ON product(featured);
CREATE INDEX IF NOT EXISTS idx_product_sku      ON product(sku) WHERE sku IS NOT NULL;

CREATE TABLE IF NOT EXISTS product_color (
    product_id UUID   NOT NULL,
    color      VARCHAR(64),
    CONSTRAINT fk_product_color_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_color_product_id ON product_color(product_id);
