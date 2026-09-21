-- V4: foydalanuvchini bloklash uchun maydonlar qo'shish
ALTER TABLE users ADD COLUMN IF NOT EXISTS block_reason VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMP;
