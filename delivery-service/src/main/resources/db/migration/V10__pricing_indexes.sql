-- Audit #7: admin ro'yxatlar so'rovlari uchun indekslar
-- (repository'lar ORDER BY priority / min_order / threshold_amount bo'yicha saralaydi)

CREATE INDEX IF NOT EXISTS idx_pricing_rules_priority_created
    ON delivery_pricing_rules(priority, created_at);

CREATE INDEX IF NOT EXISTS idx_surcharges_priority_created
    ON delivery_surcharges(priority, created_at);

CREATE INDEX IF NOT EXISTS idx_free_delivery_rules_min_order
    ON free_delivery_rules(min_order);

CREATE INDEX IF NOT EXISTS idx_cart_incentive_rules_threshold
    ON cart_incentive_rules(threshold_amount);

CREATE INDEX IF NOT EXISTS idx_delivery_zone_priority
    ON delivery_zone(priority);
