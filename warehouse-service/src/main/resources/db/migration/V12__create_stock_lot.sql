-- Lot-based cost tracking: each purchase line creates a lot with its own unit cost.
-- FIFO consumption decrements lots; COGS is derived from consumed lot cost.

CREATE TABLE IF NOT EXISTS stock_lot (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(38,2) NOT NULL,
    unit_cost NUMERIC(38,2) NOT NULL DEFAULT 0,
    purchase_id UUID,
    purchase_item_id UUID,
    received_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_lot_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id),
    CONSTRAINT fk_stock_lot_purchase FOREIGN KEY (purchase_id) REFERENCES purchase (id) ON DELETE SET NULL,
    CONSTRAINT fk_stock_lot_purchase_item FOREIGN KEY (purchase_item_id) REFERENCES purchase_item (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_stock_lot_wh_product ON stock_lot (warehouse_id, product_id);
CREATE INDEX IF NOT EXISTS idx_stock_lot_received ON stock_lot (received_date);

-- Cost snapshots on stock movements so COGS is auditable per movement.
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(38,2);
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS total_cost NUMERIC(38,2);
