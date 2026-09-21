-- Har mahsulotga unikal yetkazish muddati:
-- delivery_days_min = omborda bor holatda (masalan 2),
-- delivery_days_max = zavoddan zakaz berilganda (masalan 5).
ALTER TABLE product ADD COLUMN delivery_days_min INTEGER;
ALTER TABLE product ADD COLUMN delivery_days_max INTEGER;
