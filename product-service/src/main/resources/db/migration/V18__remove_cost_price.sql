-- Tannarx endi ombor (warehouse) da, product_price.cost_price keraksiz
ALTER TABLE product_price DROP COLUMN IF EXISTS cost_price;
