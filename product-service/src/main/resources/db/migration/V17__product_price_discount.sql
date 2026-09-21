-- V17: ProductPrice ga vaqt chegarali chegirma qo'shish.
-- Semantika: sale_price = asl (chegirmasiz) narx; discount_percent + discount_start_date/end_date
-- faol bo'lsa, joriy narx = sale_price * (100 - discount_percent) / 100.
-- Muddat o'tgach (yoki start hali kelmagan bo'lsa) chegirma avtomatik o'chadi —
-- hisoblash o'qish vaqtida (now) asosida amalga oshiriladi, scheduler kerak emas.

ALTER TABLE product_price
    ADD COLUMN IF NOT EXISTS discount_percent   INTEGER,
    ADD COLUMN IF NOT EXISTS discount_start_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS discount_end_date   TIMESTAMP;

COMMENT ON COLUMN product_price.discount_percent IS 'Chegirma foizi (0..100). Null = chegirma yo''q';
COMMENT ON COLUMN product_price.discount_start_date IS 'Chegirma boshlanish sanasi (null = cheksiz oldin)';
COMMENT ON COLUMN product_price.discount_end_date IS 'Chegirma tugash sanasi (null = cheksiz)';