-- delivery-service ichki SMS shablonlari (telefon delivery yozuvida).
INSERT INTO notification_template (id, type, channel, subject, body_template, active)
VALUES
    (gen_random_uuid(), 'DELIVERY_ASSIGNED', 'SMS', 'Kuryer biriktirildi',
     'Buyurtma #{orderNumber}: kuryer #{courierName}. Kuzatuv: #{trackingCode}', true),
    (gen_random_uuid(), 'DELIVERY_STATUS_UPDATED', 'SMS', 'Yetkazish holati',
     '#{trackingCode}: #{status}', true),
    (gen_random_uuid(), 'DELIVERY_COMPLETED', 'SMS', 'Yetkazildi',
     'Buyurtma #{orderNumber} yetkazildi. Rahmat!', true),
    (gen_random_uuid(), 'DELIVERY_FAILED', 'SMS', 'Yetkazilmadi',
     '#{trackingCode}: #{reason}', true)
ON CONFLICT (type) DO UPDATE SET
    channel       = EXCLUDED.channel,
    subject       = EXCLUDED.subject,
    body_template = EXCLUDED.body_template,
    active        = EXCLUDED.active;
