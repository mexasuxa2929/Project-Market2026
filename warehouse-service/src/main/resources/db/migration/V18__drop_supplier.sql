ALTER TABLE purchase DROP CONSTRAINT IF EXISTS fk_purchase_supplier;
ALTER TABLE purchase DROP COLUMN IF EXISTS supplier_id;
DROP TABLE IF EXISTS supplier;
