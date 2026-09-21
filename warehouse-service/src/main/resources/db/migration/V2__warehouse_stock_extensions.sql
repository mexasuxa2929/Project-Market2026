ALTER TABLE warehouse
    ADD COLUMN IF NOT EXISTS capacity INTEGER;

CREATE TABLE IF NOT EXISTS stock_movement (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(32),
    reason VARCHAR(500),
    actor_user_id UUID,
    actor_username VARCHAR(128),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS stock_return (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    order_id UUID,
    returned_by UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    approved_by UUID
);

CREATE TABLE IF NOT EXISTS stock_return_item (
    id UUID PRIMARY KEY,
    return_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,6) NOT NULL,
    condition VARCHAR(16) NOT NULL,
    CONSTRAINT fk_stock_return_item_return FOREIGN KEY (return_id) REFERENCES stock_return(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_session (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_by UUID NOT NULL,
    completed_by UUID,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    note VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS inventory_item (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    product_id UUID NOT NULL,
    system_quantity NUMERIC(19,6) NOT NULL,
    counted_quantity NUMERIC(19,6),
    difference NUMERIC(19,6),
    status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_inventory_item_session FOREIGN KEY (session_id) REFERENCES inventory_session(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_stock_movement_wh_created ON stock_movement (warehouse_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movement_product ON stock_movement (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_return_wh_created ON stock_return (warehouse_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_return_item_return ON stock_return_item (return_id);
CREATE INDEX IF NOT EXISTS idx_inventory_session_wh_started ON inventory_session (warehouse_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_item_session ON inventory_item (session_id);
