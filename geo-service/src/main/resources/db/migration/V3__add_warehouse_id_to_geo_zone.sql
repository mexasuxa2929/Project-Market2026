ALTER TABLE geo_zone ADD COLUMN warehouse_id UUID;
CREATE INDEX idx_geo_zone_warehouse_id ON geo_zone(warehouse_id);
