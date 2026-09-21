-- Revert V15: cancelled lots are hard-deleted again, flags no longer needed.
ALTER TABLE stock_lot DROP COLUMN IF EXISTS cancelled;
ALTER TABLE purchase DROP COLUMN IF EXISTS cancelled;
