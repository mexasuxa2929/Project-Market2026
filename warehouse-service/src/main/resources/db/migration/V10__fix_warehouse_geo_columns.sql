-- V9 eski versiyada lat/lng/geoGroupId qo'shilgan bo'lsa, ularni olib tashlaymiz
ALTER TABLE warehouse DROP COLUMN IF EXISTS latitude;
ALTER TABLE warehouse DROP COLUMN IF EXISTS longitude;
ALTER TABLE warehouse DROP COLUMN IF EXISTS geo_group_id;

-- geo_zone_id hali mavjud bo'lmasa qo'shamiz
ALTER TABLE warehouse ADD COLUMN IF NOT EXISTS geo_zone_id UUID;
CREATE INDEX IF NOT EXISTS idx_warehouse_geo_zone_id ON warehouse(geo_zone_id);
