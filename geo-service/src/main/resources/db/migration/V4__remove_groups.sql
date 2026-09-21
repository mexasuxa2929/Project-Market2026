DROP TABLE IF EXISTS geo_zone_group CASCADE;
ALTER TABLE geo_zone DROP COLUMN IF EXISTS group_id;
DROP INDEX IF EXISTS idx_geo_zone_group_id;
