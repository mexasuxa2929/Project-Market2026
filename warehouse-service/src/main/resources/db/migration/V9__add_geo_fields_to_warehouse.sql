ALTER TABLE warehouse ADD COLUMN geo_zone_id UUID;
CREATE INDEX idx_warehouse_geo_zone_id ON warehouse(geo_zone_id);

