CREATE TABLE courier (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    region VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_deliveries INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE delivery_zone (
    id UUID PRIMARY KEY,
    region VARCHAR(128) NOT NULL,
    district VARCHAR(128) NOT NULL,
    fee NUMERIC(19,2) NOT NULL,
    estimated_days INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE delivery (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    courier_id UUID REFERENCES courier(id),
    status VARCHAR(16) NOT NULL,
    from_warehouse_id UUID NOT NULL,
    delivery_address VARCHAR(600) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(64) NOT NULL,
    region VARCHAR(128) NOT NULL,
    district VARCHAR(128) NOT NULL,
    estimated_delivery DATE NOT NULL,
    actual_delivery TIMESTAMP,
    delivery_fee NUMERIC(19,2) NOT NULL,
    tracking_code VARCHAR(32) NOT NULL UNIQUE,
    note VARCHAR(500),
    fail_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE delivery_tracking_event (
    id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL REFERENCES delivery(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    location VARCHAR(200),
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_delivery_order_id ON delivery(order_id);
CREATE INDEX idx_delivery_courier_id ON delivery(courier_id);
CREATE INDEX idx_delivery_tracking_code ON delivery(tracking_code);
CREATE INDEX idx_tracking_delivery_id ON delivery_tracking_event(delivery_id);

INSERT INTO delivery_zone (id, region, district, fee, estimated_days) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Toshkent', 'Yunusobod', 15000, 1),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Toshkent', 'Chilonzor', 15000, 1),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Samarqand', 'Samarqand', 20000, 2);
