-- Refresh tokenni qurilmaga bog'lash (device binding) uchun user_sessions jadvaliga device_id ustuni.
-- Mavjud qatorlar uchun NULL bo'ladi (legacy sessiyalar); keyingi refresh'da bog'lanadi.

ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS device_id VARCHAR(256);

CREATE INDEX IF NOT EXISTS idx_user_sessions_device_id ON user_sessions (device_id);
