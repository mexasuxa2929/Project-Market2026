-- V10: foydalanuvchi to'liq ismi (full_name) maydonini qo'shish
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);