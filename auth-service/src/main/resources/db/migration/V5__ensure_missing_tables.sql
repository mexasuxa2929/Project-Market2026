-- V5: etishmayotgan jadvallarni qayta yaratish (DB sxemasi Flyway tarixi bilan mos kelmagan holat uchun)

CREATE TABLE IF NOT EXISTS email_otp (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(100),
    password_hash VARCHAR(255),
    code_hash VARCHAR(64) NOT NULL,
    attempts_used INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

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

CREATE INDEX IF NOT EXISTS idx_email_otp_email ON email_otp (email);
CREATE INDEX IF NOT EXISTS idx_email_otp_expires_at ON email_otp (expires_at);
CREATE INDEX IF NOT EXISTS idx_email_otp_username ON email_otp (username);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions (active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_hash ON user_sessions (refresh_token_hash);
