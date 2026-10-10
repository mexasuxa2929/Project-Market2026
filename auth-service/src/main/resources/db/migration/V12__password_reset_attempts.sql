ALTER TABLE password_reset_tokens ADD COLUMN IF NOT EXISTS attempts_used INT NOT NULL DEFAULT 0;
