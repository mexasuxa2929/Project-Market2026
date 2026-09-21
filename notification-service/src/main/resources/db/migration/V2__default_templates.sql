-- Standart shablonlar (mavjud type bo'lsa yangilanadi).
-- V1 dagi ORDER_SHIPPED / PAYMENT_FAILED namunalari admin ro'yxatida 5 ta tur bilan moslash uchun olib tashlanadi.
DELETE FROM notification_template WHERE type IN ('ORDER_SHIPPED', 'PAYMENT_FAILED');

INSERT INTO notification_template (id, type, channel, subject, body_template, active)
VALUES
    (gen_random_uuid(), 'ORDER_CREATED', 'EMAIL', 'Buyurtmangiz qabul qilindi',
     'Buyurtma #{orderNumber} qabul qilindi. Jami: #{totalAmount}', true),
    (gen_random_uuid(), 'ORDER_CONFIRMED', 'EMAIL', 'Buyurtmangiz tasdiqlandi',
     'Buyurtma #{orderNumber} tasdiqlandi.', true),
    (gen_random_uuid(), 'ORDER_CANCELLED', 'EMAIL', 'Buyurtmangiz bekor qilindi',
     'Buyurtma #{orderNumber} bekor qilindi.', true),
    (gen_random_uuid(), 'LOW_STOCK_ALERT', 'EMAIL', 'Kam qoldiq ogohlantirishi',
     '#{productName} mahsuloti kam qoldi.', true),
    (gen_random_uuid(), 'PAYMENT_SUCCESS', 'EMAIL', 'To''lov muvaffaqiyatli',
     'Buyurtma #{orderNumber} uchun to''lov qabul qilindi.', true)
ON CONFLICT (type) DO UPDATE SET
    channel      = EXCLUDED.channel,
    subject      = EXCLUDED.subject,
    body_template = EXCLUDED.body_template,
    active       = EXCLUDED.active;
