-- M-16: order yaratishda idempotency — dublikat POST (retry/tarmoq xatosi)
-- qayta order yaratmaydi; (user_id, idempotency_key) unique.
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX uq_orders_user_idempotency
    ON orders (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
