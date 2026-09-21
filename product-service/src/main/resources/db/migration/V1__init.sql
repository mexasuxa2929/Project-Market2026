CREATE TABLE IF NOT EXISTS product (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    barcode VARCHAR(64) NOT NULL,
    category_id UUID,
    brand_id UUID,
    manufacturer_id UUID,
    unit VARCHAR(32) NOT NULL,
    lead_time_days INTEGER NOT NULL,
    min_stock INTEGER NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    length DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    package_type VARCHAR(255),
    fragile BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS product_image (
    product_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    relative_path VARCHAR(512),
    PRIMARY KEY (sort_order, product_id),
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS product_price (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    cost_price NUMERIC(38, 2) NOT NULL,
    sale_price NUMERIC(38, 2) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_product_barcode ON product(barcode);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_brand_id ON product(brand_id);
CREATE INDEX IF NOT EXISTS idx_product_price_product_id ON product_price(product_id);
