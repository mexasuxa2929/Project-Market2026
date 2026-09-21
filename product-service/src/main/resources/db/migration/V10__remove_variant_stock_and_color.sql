-- Remove stock column from product_variant (stock is now managed in warehouse-service)
ALTER TABLE product_variant DROP COLUMN IF EXISTS stock;

-- Deprecate product_color table: drop it since colors are now derived from variant attributes.
-- Existing color data is superseded by variant.attributes (e.g. attributes->>'rang').
-- The @ElementCollection mapping has been removed from the Product entity.
DROP TABLE IF EXISTS product_color;
