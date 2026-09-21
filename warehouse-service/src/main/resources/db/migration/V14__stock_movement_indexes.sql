-- Speed up reverseSalesOut (filters by warehouse_id + reference_type + reference_id + movement_type)
-- and the profit report (filters by warehouse_id + movement_type).
CREATE INDEX IF NOT EXISTS idx_stock_movement_reference ON stock_movement (warehouse_id, reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_type ON stock_movement (warehouse_id, movement_type);