-- D-H2: surcharge jadvali vaqtiy (schedule) ustunlari — HOLIDAY/PEAK faqat berilgan davrga faol.
ALTER TABLE delivery_surcharges ADD COLUMN start_at TIMESTAMP;
ALTER TABLE delivery_surcharges ADD COLUMN end_at TIMESTAMP;

-- D-H5/D-H4: analytics event qaysi qoida va qaysi yoldan kelganini yozadi.
ALTER TABLE delivery_pricing_events ADD COLUMN rule_id UUID;
ALTER TABLE delivery_pricing_events ADD COLUMN rule_name VARCHAR(128);
ALTER TABLE delivery_pricing_events ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'CREATE';
ALTER TABLE delivery_pricing_events ADD COLUMN surcharge_names TEXT;

CREATE INDEX IF NOT EXISTS idx_pricing_events_source ON delivery_pricing_events (source);
CREATE INDEX IF NOT EXISTS idx_pricing_events_threshold ON delivery_pricing_events (threshold_amount);
CREATE INDEX IF NOT EXISTS idx_pricing_events_rule ON delivery_pricing_events (rule_name);

-- D-H7: buzuvchi amallar (zona o'chirish) uchun sabab.
ALTER TABLE delivery_zone_audit ADD COLUMN reason VARCHAR(255);
