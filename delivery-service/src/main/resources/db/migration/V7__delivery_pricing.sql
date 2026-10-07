-- ============================================================
-- Delivery Pricing & Cart Incentive Management System (core)
-- ============================================================

-- Zona qo'shimcha tariflari (min/max klamp + priority)
ALTER TABLE delivery_zone ADD COLUMN min_fee NUMERIC(19,2);
ALTER TABLE delivery_zone ADD COLUMN max_fee NUMERIC(19,2);
ALTER TABLE delivery_zone ADD COLUMN priority INTEGER NOT NULL DEFAULT 100;

-- ------------------------------------------------------------
-- 1) Asosiy pricing qoidalari (formula: base + distance*perKm, clamp min/max)
-- ------------------------------------------------------------
CREATE TABLE delivery_pricing_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    zone_id UUID REFERENCES delivery_zone(id) ON DELETE SET NULL,
    delivery_type VARCHAR(32),
    base_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    price_per_km NUMERIC(19,2) NOT NULL DEFAULT 0,
    price_per_kg NUMERIC(19,2) NOT NULL DEFAULT 0,
    min_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    max_fee NUMERIC(19,2),
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    change_reason VARCHAR(255),
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pricing_rules_active ON delivery_pricing_rules(active);

-- ------------------------------------------------------------
-- 2) Free delivery (threshold) qoidalari
-- ------------------------------------------------------------
CREATE TABLE free_delivery_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    min_order NUMERIC(19,2) NOT NULL,
    delivery_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    zone_id UUID REFERENCES delivery_zone(id) ON DELETE SET NULL,
    delivery_type VARCHAR(32),
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 3) Cart incentive (kattaroq buyurtmaga undash) qoidalari
-- ------------------------------------------------------------
CREATE TABLE cart_incentive_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    threshold_amount NUMERIC(19,2) NOT NULL,
    reward_type VARCHAR(16) NOT NULL DEFAULT 'FREE',   -- FREE | AMOUNT | PERCENT
    reward_value NUMERIC(19,2) NOT NULL DEFAULT 0,
    message_template VARCHAR(500) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 4) Delivery turlari (STANDARD / EXPRESS / SCHEDULED)
-- ------------------------------------------------------------
CREATE TABLE delivery_types (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    base_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    additional_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    estimated_minutes INTEGER NOT NULL DEFAULT 60,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO delivery_types (id, name, base_fee, additional_fee, estimated_minutes, active) VALUES
('d1000000-0000-4000-8000-000000000001', 'STANDARD', 0, 0, 60, TRUE),
('d1000000-0000-4000-8000-000000000002', 'EXPRESS',  0, 5000, 25, TRUE),
('d1000000-0000-4000-8000-000000000003', 'SCHEDULED', 0, 0, 180, TRUE);

-- ------------------------------------------------------------
-- 5) Surcharges (peak hour, og'irlik, express, night, holiday...)
-- ------------------------------------------------------------
CREATE TABLE delivery_surcharges (
    id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    condition_type VARCHAR(24) NOT NULL,   -- PEAK_HOUR | NIGHT | HOLIDAY | HEAVY_ORDER | EXPRESS | HIGH_DEMAND | REMOTE_AREA | WEATHER
    operator VARCHAR(8) NOT NULL DEFAULT 'GTE',  -- GT | GTE | LT | LTE | EQ
    condition_value NUMERIC(19,2),
    fee NUMERIC(19,2) NOT NULL,
    fee_type VARCHAR(8) NOT NULL DEFAULT 'FIXED',  -- FIXED | PERCENT
    time_from TIME,
    time_to TIME,
    weight_from NUMERIC(10,2),
    weight_to NUMERIC(10,2),
    delivery_type VARCHAR(32),
    priority INTEGER NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 6) Global pricing sozlamalari (real cost hisobida margin uchun)
-- ------------------------------------------------------------
CREATE TABLE delivery_pricing_settings (
    id UUID PRIMARY KEY,
    base_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    per_km_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    per_kg_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'UZS',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO delivery_pricing_settings (id, base_cost, per_km_cost, per_kg_cost, currency) VALUES
('e1000000-0000-4000-8000-000000000001', 14000, 1000, 1000, 'UZS');

-- ------------------------------------------------------------
-- 7) Pricing versioning + audit
-- ------------------------------------------------------------
CREATE TABLE pricing_versions (
    id UUID PRIMARY KEY,
    version INTEGER NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- DRAFT | ACTIVE | ARCHIVED
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_from TIMESTAMP,
    effective_until TIMESTAMP,
    change_reason VARCHAR(500)
);

CREATE TABLE pricing_audit_logs (
    id UUID PRIMARY KEY,
    admin_name VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(48) NOT NULL,
    entity_id UUID,
    old_value TEXT,
    new_value TEXT,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pricing_audit_created ON pricing_audit_logs(created_at);

-- ------------------------------------------------------------
-- 8) Analytics eventlari (har bir hisoblash yoziladi)
-- ------------------------------------------------------------
CREATE TABLE delivery_pricing_events (
    id UUID PRIMARY KEY,
    order_id UUID,
    cart_total NUMERIC(19,2),
    distance_km NUMERIC(10,2),
    weight_kg NUMERIC(10,2),
    zone_id UUID,
    delivery_type VARCHAR(32),
    base_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    distance_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    weight_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    surcharge_total NUMERIC(19,2) NOT NULL DEFAULT 0,
    discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    final_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    estimated_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    margin NUMERIC(19,2) NOT NULL DEFAULT 0,
    free_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    threshold_amount NUMERIC(19,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pricing_events_created ON delivery_pricing_events(created_at);
CREATE INDEX idx_pricing_events_order ON delivery_pricing_events(order_id);
