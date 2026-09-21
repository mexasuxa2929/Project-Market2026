-- Soft-cancel support: cancelled purchases/lots stay visible as history
-- but are excluded from stock math and the active purchases list.

ALTER TABLE stock_lot ADD COLUMN IF NOT EXISTS cancelled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE purchase ADD COLUMN IF NOT EXISTS cancelled BOOLEAN NOT NULL DEFAULT FALSE;
