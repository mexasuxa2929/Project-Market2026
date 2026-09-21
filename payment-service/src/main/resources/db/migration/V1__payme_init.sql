CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payme_transactions (
    id BIGSERIAL PRIMARY KEY,
    paycom_tx_id VARCHAR(128) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    amount BIGINT NOT NULL,
    state SMALLINT NOT NULL,
    reason INTEGER,
    create_time BIGINT NOT NULL,
    perform_time BIGINT,
    cancel_time BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_payme_transactions_order_id ON payme_transactions (order_id);
CREATE INDEX ix_payme_transactions_state ON payme_transactions (state);

CREATE TABLE payme_request_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(128),
    method VARCHAR(128),
    request_payload VARCHAR(16384) NOT NULL,
    response_payload VARCHAR(16384) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
