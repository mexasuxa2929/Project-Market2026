CREATE TABLE operating_expense (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL REFERENCES warehouse(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(8) NOT NULL DEFAULT 'UZS',
    description VARCHAR(512),
    incurred_at TIMESTAMP NOT NULL,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_operating_expense_wh_date ON operating_expense(warehouse_id, incurred_at);
CREATE INDEX idx_operating_expense_category ON operating_expense(category);
