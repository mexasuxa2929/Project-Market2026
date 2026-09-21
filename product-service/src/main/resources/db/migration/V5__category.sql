CREATE TABLE IF NOT EXISTS category (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    parent_id   UUID         REFERENCES category(id) ON DELETE SET NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_category_name ON category(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON category(parent_id);
