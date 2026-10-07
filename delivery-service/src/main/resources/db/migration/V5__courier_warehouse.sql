-- Kuryerni omborga ID orqali biriktirish: boshqa omborlarga chalkashib ketmasligi uchun.
-- Mavjud kuryerlar region'da ombor nomini saqlashi mumkin — ular NULL (biriktirilmagan) qoladi,
-- admin ularni ombor ichidan qayta biriktiradi.
ALTER TABLE courier ADD COLUMN warehouse_id UUID;
CREATE INDEX IF NOT EXISTS idx_courier_warehouse_id ON courier(warehouse_id);
