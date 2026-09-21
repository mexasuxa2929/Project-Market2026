CREATE TABLE IF NOT EXISTS warehouse (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255),
    contact_info VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS warehouse_stock (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(38,2) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(38,2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_warehouse_stock_wh_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT fk_warehouse_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
);

CREATE TABLE IF NOT EXISTS warehouse_admin (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT uk_warehouse_admin_wh_user UNIQUE (warehouse_id, user_id),
    CONSTRAINT fk_warehouse_admin_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
);

CREATE TABLE IF NOT EXISTS purchase (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    purchase_date TIMESTAMP NOT NULL,
    invoice_number VARCHAR(255),
    total_amount NUMERIC(38,2),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT fk_purchase_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id)
);

CREATE TABLE IF NOT EXISTS purchase_item (
    id UUID PRIMARY KEY,
    purchase_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(38,2) NOT NULL,
    unit_price NUMERIC(38,2) NOT NULL,
    total_price NUMERIC(38,2),
    CONSTRAINT fk_purchase_item_purchase FOREIGN KEY (purchase_id) REFERENCES purchase (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    warehouse_id UUID,
    actor_user_id UUID,
    actor_username VARCHAR(128),
    details VARCHAR(2000),
    created_at TIMESTAMP NOT NULL
);
