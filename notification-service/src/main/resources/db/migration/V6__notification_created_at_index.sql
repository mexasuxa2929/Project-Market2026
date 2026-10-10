-- M-30: bildirishnoma ro'yxati created_at bo'yicha saralanadi — indeks (full scan yo'qoladi)
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification (created_at DESC);
