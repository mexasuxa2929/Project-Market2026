CREATE TABLE geo_zone (
    id              UUID PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    region          VARCHAR(128),
    district        VARCHAR(128),
    polygon         TEXT,
    center_lat      VARCHAR(32),
    center_lng      VARCHAR(32),
    zoom_level      INTEGER DEFAULT 12,
    fee             NUMERIC(19, 2) NOT NULL,
    estimated_days  INTEGER NOT NULL DEFAULT 1,
    color           VARCHAR(16) NOT NULL DEFAULT '#6366F1',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
