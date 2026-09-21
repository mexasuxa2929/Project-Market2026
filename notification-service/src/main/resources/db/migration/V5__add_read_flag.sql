ALTER TABLE notification ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_notification_user_read ON notification(user_id, is_read);