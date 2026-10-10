-- M-10: payment-confirmed xabari bilan kutilgan summa (som) ham yuboriladi;
-- order-service to'lov summasiz PAID o'tishni bloklaydi.
ALTER TABLE payment_order_notify_outbox ADD COLUMN amount_som NUMERIC(19, 2);
