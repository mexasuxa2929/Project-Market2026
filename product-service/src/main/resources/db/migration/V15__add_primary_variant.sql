-- Rang guruhi (group_id) dublikatlarini ro'yxatda birlashtirish uchun "vakil" (representative) belgisi.
-- Har bir group_id guruhida aynan bitta yozuv primary_variant = true bo'ladi; ro'yxat endpointi
-- (GET /api/products) faqat shu vakillarni qaytaradi, qolgan rang nusxalari edit oynasidagi
-- "RANGLAR GURUHI" bo'limi orqali ko'rinadi. Guruhsiz (standalone) mahsulotlar ham vakil bo'ladi.

ALTER TABLE product ADD COLUMN IF NOT EXISTS primary_variant BOOLEAN NOT NULL DEFAULT true;

-- Backfill: mavjud rang guruhlaridagi dublikatlarni ham tuzatadi.
-- 1) Guruhga tegishli barcha yozuvlarni avval "vakil emas" qilamiz.
UPDATE product SET primary_variant = false WHERE group_id IS NOT NULL;

-- 2) Har guruhda eng erta yaratilgan (created_at, id bo'yicha) yozuvni vakil qilamiz.
UPDATE product p SET primary_variant = true
WHERE p.group_id IS NOT NULL
  AND p.id = (
    SELECT p2.id FROM product p2
    WHERE p2.group_id = p.group_id
    ORDER BY p2.created_at ASC, p2.id ASC
    LIMIT 1
  );

-- Ro'yxat filtri primary_variant bo'yicha ishlagani uchun indeks qo'shamiz.
CREATE INDEX IF NOT EXISTS idx_product_primary_variant ON product(primary_variant);
