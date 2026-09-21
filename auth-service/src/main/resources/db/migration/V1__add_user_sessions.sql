CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    device_info VARCHAR(512),
    ip_address VARCHAR(128),
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions (active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_hash ON user_sessions (refresh_token_hash);
