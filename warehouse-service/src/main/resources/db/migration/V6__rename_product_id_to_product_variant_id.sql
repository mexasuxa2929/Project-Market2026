-- Rename product_id -> product_variant_id in warehouse_stock
ALTER TABLE warehouse_stock RENAME COLUMN product_id TO product_variant_id;

-- Update unique constraint (drop old, add new)
ALTER TABLE warehouse_stock DROP CONSTRAINT IF EXISTS uk_warehouse_stock_wh_product;
ALTER TABLE warehouse_stock ADD CONSTRAINT uk_warehouse_stock_wh_variant UNIQUE (warehouse_id, product_variant_id);

-- Rename product_id -> product_variant_id in stock_movement
ALTER TABLE stock_movement RENAME COLUMN product_id TO product_variant_id;

-- Update index if it exists
DROP INDEX IF EXISTS idx_stock_movement_product_id;
CREATE INDEX IF NOT EXISTS idx_stock_movement_product_variant_id ON stock_movement(product_variant_id);

-- Rename product_id -> product_variant_id in purchase_item
ALTER TABLE purchase_item RENAME COLUMN product_id TO product_variant_id;

-- Rename product_id -> product_variant_id in stock_return_item (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'stock_return_item' AND column_name = 'product_id') THEN
        ALTER TABLE stock_return_item RENAME COLUMN product_id TO product_variant_id;
    END IF;
END$$;

-- Rename product_id -> product_variant_id in inventory_item (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'inventory_item' AND column_name = 'product_id') THEN
        ALTER TABLE inventory_item RENAME COLUMN product_id TO product_variant_id;
    END IF;
END$$;

-- DATA MIGRATION NOTE:
-- Existing warehouse_stock rows have product_id values that pointed to Product.id.
-- After this migration they are stored in product_variant_id.
-- Each legacy product should have had a "default variant" created by the product-service
-- ensureDefaultVariant() method (triggered on createProduct).
-- For existing products without variants, run the following once after deploying product-service V10:
--   INSERT INTO product_variant (id, product_id, sku, name, attributes, price_modifier, active)
--   SELECT gen_random_uuid(), p.id, p.barcode || '-DEFAULT', p.name, '{}', 0, true
--   FROM product p
--   WHERE NOT EXISTS (SELECT 1 FROM product_variant pv WHERE pv.product_id = p.id);
--
-- Then update warehouse_stock to use the new variant IDs:
--   UPDATE warehouse_stock ws
--   SET product_variant_id = (
--     SELECT pv.id FROM product_variant pv WHERE pv.product_id = ws.product_variant_id LIMIT 1
--   )
--   WHERE EXISTS (
--     SELECT 1 FROM product_variant pv WHERE pv.product_id = ws.product_variant_id
--   );
