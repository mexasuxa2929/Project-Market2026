-- M-30: order filtrlari uchun indekslar (payment_status, closed_at)
CREATE INDEX IF NOT EXISTS idx_order_closed_at ON orders (closed_at);
CREATE INDEX IF NOT EXISTS idx_order_payment_status ON orders (payment_status);
