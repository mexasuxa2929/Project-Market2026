-- Variantlar tizimi olib tashlandi: endi har rang mustaqil Product yozuvi hisoblanadi.
-- product-service'da ProductVariant sub-entity butunlay o'chirildi (V12__remove_variants_add_color_group.sql).
-- Shu sababli warehouse-service'dagi barcha "product_variant_id" ustunlari V6 migratsiyasidan oldingi
-- holatga — "product_id" nomiga — qaytariladi. Bu safar variant tizimi qaytmaydi, shuning uchun
-- eski nom butunlay tashlab yuboriladi (faqat rename, ehtiyot saqlovchi qoldiq ustunlar yo'q).

-- warehouse_stock: product_variant_id -> product_id
ALTER TABLE warehouse_stock RENAME COLUMN product_variant_id TO product_id;

ALTER TABLE warehouse_stock DROP CONSTRAINT IF EXISTS uk_warehouse_stock_wh_variant;
ALTER TABLE warehouse_stock ADD CONSTRAINT uk_warehouse_stock_wh_product UNIQUE (warehouse_id, product_id);

-- stock_movement: product_variant_id -> product_id
ALTER TABLE stock_movement RENAME COLUMN product_variant_id TO product_id;

DROP INDEX IF EXISTS idx_stock_movement_product_variant_id;
CREATE INDEX IF NOT EXISTS idx_stock_movement_product_id ON stock_movement(product_id);

-- purchase_item: product_variant_id -> product_id
ALTER TABLE purchase_item RENAME COLUMN product_variant_id TO product_id;

-- stock_return_item: product_variant_id -> product_id (agar ustun mavjud bo'lsa)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'stock_return_item' AND column_name = 'product_variant_id') THEN
        ALTER TABLE stock_return_item RENAME COLUMN product_variant_id TO product_id;
    END IF;
END$$;

-- inventory_item: bu jadvalda ikkita ustun bor edi — V6 dan qolgan "product_variant_id"
-- va V7 da qo'shilgan "product_id". Endi faqat "product_id" qoladi, eskisi tashlanadi.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'inventory_item' AND column_name = 'product_variant_id') THEN
        -- Agar biror sababdan product_id bo'sh (NULL) bo'lib, product_variant_id'da qiymat bo'lsa,
        -- ma'lumotni yo'qotmaslik uchun avval ko'chiramiz.
        UPDATE inventory_item
        SET product_id = product_variant_id
        WHERE product_id IS NULL AND product_variant_id IS NOT NULL;

        ALTER TABLE inventory_item DROP COLUMN product_variant_id;
    END IF;
END$$;

ALTER TABLE inventory_item ALTER COLUMN product_id SET NOT NULL;
