-- Orderning yopilgan vaqti: daromad shu sanaga bog'lab hisoblanadi.
ALTER TABLE orders ADD COLUMN closed_at TIMESTAMP NULL;
