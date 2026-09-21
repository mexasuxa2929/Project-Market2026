ALTER TABLE orders ADD COLUMN order_service_id UUID;

CREATE INDEX ix_orders_order_service_id ON orders (order_service_id);
