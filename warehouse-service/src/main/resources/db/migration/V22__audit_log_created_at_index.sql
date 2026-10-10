-- M-30: audit_log ko'p ishlatiladigan qidiruv ustuni — created_at indeksi (full scan yo'qoladi)
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
