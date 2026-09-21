CREATE TABLE IF NOT EXISTS brand_logo (
    brand_id   UUID         PRIMARY KEY,
    path       VARCHAR(512) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
