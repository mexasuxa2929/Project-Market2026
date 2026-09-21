-- Payment method chosen by customer at checkout (CASH | CARD | ONLINE, default CASH).
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(32);
