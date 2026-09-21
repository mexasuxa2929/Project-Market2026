-- Courier user link is optional: couriers can be created by admin without a
-- pre-existing auth user, and linked later via update.
ALTER TABLE courier ALTER COLUMN user_id DROP NOT NULL;
