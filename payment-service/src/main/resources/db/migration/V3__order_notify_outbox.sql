-- H4: order-service'ga payment-confirmed/payment-failed xabari yetkazilmagan
-- bo'lsa (xato/timeout/servis pastda), outbox jadvalida qoladi va
-- @Scheduled retry orqali qayta yuboriladi; reconcile job yo'qolgan
-- xabarlarni (crash oynasi) qayta topadi.

CREATE TABLE payment_order_notify_outbox (
    id               UUID PRIMARY KEY,
    order_service_id UUID NOT NULL,
    action           VARCHAR(32) NOT NULL,        -- payment-confirmed | payment-failed
    attempts         INT NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMP NOT NULL,
    sent_at          TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_payment_notify_pending
    ON payment_order_notify_outbox (order_service_id, action)
    WHERE sent_at IS NULL;

CREATE INDEX idx_payment_notify_due
    ON payment_order_notify_outbox (next_attempt_at)
    WHERE sent_at IS NULL;
