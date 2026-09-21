-- V4: review enhancements
-- 1. username (review yozgan foydalanuvchi nomi — yozish vaqtida saqlanadi)
-- 2. updated_at (oxirgi tahrirlash vaqti)

ALTER TABLE shop_product_review
    ADD COLUMN IF NOT EXISTS username    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP;

-- Mavjud yozuvlar uchun updated_at = created_at
UPDATE shop_product_review SET updated_at = created_at WHERE updated_at IS NULL;
