CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    delivery_address_id UUID NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    delivery_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'UZS',
    note VARCHAR(1000),
    cancel_reason VARCHAR(500),
    payment_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
    payment_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variant_id UUID,
    product_name VARCHAR(150) NOT NULL,
    variant_name VARCHAR(150),
    image_url VARCHAR(512),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by UUID NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE order_sequence (
    seq_year INTEGER PRIMARY KEY,
    value BIGINT NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);
