-- Refresh token'ni sessiyaga bog'lash. Bu logout'da refresh aylantirilgan
-- (eski) token yuborilgan taqdirda ham sessiyani deaktiv qilish imkonini beradi.
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS session_id UUID;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_session_id ON refresh_tokens (session_id);