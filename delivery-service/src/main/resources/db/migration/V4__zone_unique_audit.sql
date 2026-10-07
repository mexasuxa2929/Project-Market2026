-- 4-bosqich: bir xil viloyat+tuman ikki marta yaratilmasin + o'zgarishlar tarixi.
-- Avval mavjud dublikatlarni tozalash (agar bo'lsa, eng birinchisini qoldirish).
DELETE FROM delivery_zone a USING delivery_zone b
WHERE a.id > b.id
  AND LOWER(a.region) = LOWER(b.region)
  AND LOWER(a.district) = LOWER(b.district);

CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_zone_region_district
    ON delivery_zone (LOWER(region), LOWER(district));

CREATE TABLE IF NOT EXISTS delivery_zone_audit (
    id UUID PRIMARY KEY,
    zone_id UUID,
    action VARCHAR(16) NOT NULL,
    old_fee NUMERIC(19,2),
    new_fee NUMERIC(19,2),
    old_per_km_fee NUMERIC(19,2),
    new_per_km_fee NUMERIC(19,2),
    old_per_kg_fee NUMERIC(19,2),
    new_per_kg_fee NUMERIC(19,2),
    old_free_above NUMERIC(19,2),
    new_free_above NUMERIC(19,2),
    changed_by VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_zone_audit_zone_id ON delivery_zone_audit(zone_id);
