CREATE TABLE IF NOT EXISTS product_color (
    product_id UUID   NOT NULL,
    color      VARCHAR(64),
    CONSTRAINT fk_product_color_product
        FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_color_product_id ON product_color(product_id);
