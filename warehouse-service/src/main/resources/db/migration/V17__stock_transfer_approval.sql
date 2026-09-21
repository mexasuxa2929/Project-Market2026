CREATE TABLE stock_transfer (
    id UUID PRIMARY KEY,
    from_warehouse_id UUID NOT NULL,
    to_warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    unit_cost NUMERIC(19,6),
    status VARCHAR(16) NOT NULL,
    note VARCHAR(1000),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMP
);

CREATE INDEX idx_stock_transfer_to_warehouse_status ON stock_transfer(to_warehouse_id, status);
CREATE INDEX idx_stock_transfer_from_warehouse ON stock_transfer(from_warehouse_id);

ALTER TABLE stock_movement ADD COLUMN status VARCHAR(16);
