CREATE TABLE dashboard_snapshots (
    snapshot_key VARCHAR(64) PRIMARY KEY,
    payload_json TEXT NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE revenue_daily (
    revenue_date DATE PRIMARY KEY,
    revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    order_count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE top_products_cache (
    period VARCHAR(16) NOT NULL,
    rank_position INT NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(512) NOT NULL,
    sold_count BIGINT NOT NULL,
    revenue NUMERIC(19, 2) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (period, rank_position)
);
