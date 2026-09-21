CREATE TABLE geo_zone_group (
    id          UUID PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    color       VARCHAR(16),
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

ALTER TABLE geo_zone ADD COLUMN group_id UUID REFERENCES geo_zone_group(id);
CREATE INDEX idx_geo_zone_group_id ON geo_zone(group_id);
