-- Variants can now carry their own absolute price, instead of always being
-- derived from product base price + price_modifier. When variant.price is
-- set, it is used as-is; when null, callers fall back to basePrice + price_modifier
-- for backward compatibility with existing variants.
ALTER TABLE product_variant ADD COLUMN IF NOT EXISTS price NUMERIC(19,2);
