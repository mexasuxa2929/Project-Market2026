-- Delivery address is optional: orders can be placed without a pre-created
-- customer address (fallback to a plain text snapshot or none).
ALTER TABLE orders ALTER COLUMN delivery_address_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN delivery_address DROP NOT NULL;
