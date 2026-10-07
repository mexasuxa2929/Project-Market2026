-- Yetkazib berish narxini shakllantirish:
-- fee = bazaviy narx, + masofa (km * per_km_fee) + og'irlik (kg * per_kg_fee),
-- agar order subtotal free_above dan katta/eng bo'lsa — bepul (0).
ALTER TABLE delivery_zone ADD COLUMN per_km_fee NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE delivery_zone ADD COLUMN per_kg_fee NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE delivery_zone ADD COLUMN free_above NUMERIC(19,2);

-- Mavjud zonalar uchun boshlang'ich qiymatlar (admin keyin tahrirlaydi):
-- Shahar ichi km uchun 1000 so'm/km, kg uchun 2000 so'm/kg, 500 000 dan yuqori bepul.
UPDATE delivery_zone SET per_km_fee = 1000, per_kg_fee = 2000, free_above = 500000
WHERE region = 'Toshkent';
UPDATE delivery_zone SET per_km_fee = 1500, per_kg_fee = 2500, free_above = 700000
WHERE region = 'Samarqand';
