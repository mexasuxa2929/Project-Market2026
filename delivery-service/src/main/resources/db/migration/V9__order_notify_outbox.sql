-- H2 qoldiq: order-service'ga DELIVERED/FAILED xabari yuborilib bo'lmagan bo'lsa
-- (Feign/timeout), xabar yo'qolib ketmasligi uchun outbox jadvalida saqlanadi va
-- @Scheduled retry orqali qayta yuboriladi.

CREATE TABLE order_notify_outbox (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL,
    kind            VARCHAR(32) NOT NULL,          -- DELIVERED | FAILED
    reason          TEXT,
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Faqat yuborilmagan (pending) qatorlar uchun — takroriy enqueue oldini oladi.
CREATE UNIQUE INDEX uq_notify_outbox_pending
    ON order_notify_outbox (order_id, kind)
    WHERE sent_at IS NULL;

CREATE INDEX idx_notify_outbox_due
    ON order_notify_outbox (next_attempt_at)
    WHERE sent_at IS NULL;
