-- Persist COGS cost snapshots at scale 6 to match the StockMovement entity and
-- avoid rounding loss when FIFO unit costs (scale 6) are stored on movements,
-- which previously were silently rounded to 2 dp by the NUMERIC(38,2) column.
-- This keeps profit reporting and reverseSalesOut re-lotting cost-accurate.
ALTER TABLE stock_movement ALTER COLUMN unit_cost TYPE NUMERIC(19,6);
ALTER TABLE stock_movement ALTER COLUMN total_cost TYPE NUMERIC(19,6);