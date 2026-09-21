-- Variantlar tizimi olib tashlandi: endi har rang mustaqil Product yozuvi hisoblanadi.
-- Bir mahsulotning turli rangdagi nusxalari "group_id" orqali bog'lanadi.

ALTER TABLE product ADD COLUMN IF NOT EXISTS color VARCHAR(60);
ALTER TABLE product ADD COLUMN IF NOT EXISTS group_id UUID;

CREATE INDEX IF NOT EXISTS idx_product_group_id ON product(group_id);

-- Eski variant jadvalini olib tashlash (agar mavjud bo'lsa)
DROP TABLE IF EXISTS product_variant;

-- Har bir mahsulot (rang) o'ziga xos barcode'ga ega bo'lishi kerak.
-- Eslatma: agar bazada hozircha takrorlangan barcode'lar mavjud bo'lsa, bu UNIQUE INDEX yaratish muvaffaqiyatsiz tugaydi —
-- shu holda avval takrorlanган barcode'larni qo'lda tozalash kerak bo'ladi.
DROP INDEX IF EXISTS idx_product_barcode;
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_barcode ON product(barcode);
