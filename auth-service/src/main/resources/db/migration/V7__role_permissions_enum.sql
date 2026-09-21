-- V7: Replace entity-based role_permissions (UUID FK) with enum-based schema.
-- The permissions entity table and old join table are no longer needed.

DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id    UUID        NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, permission)
);
