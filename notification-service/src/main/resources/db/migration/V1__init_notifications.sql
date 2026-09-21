CREATE TABLE notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(16) NOT NULL,
    type VARCHAR(32) NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(64),
    subject VARCHAR(500),
    body TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    next_retry_at TIMESTAMP
);

CREATE TABLE notification_template (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL UNIQUE,
    channel VARCHAR(16) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_notification_user_id ON notification(user_id);
CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_next_retry ON notification(next_retry_at);

INSERT INTO notification_template (id, type, channel, subject, body_template, active) VALUES
('11111111-1111-1111-1111-111111111111','ORDER_CREATED','EMAIL','Buyurtmangiz #{orderNumber} qabul qilindi','<h3>Assalomu alaykum!</h3><p>Buyurtma: #{orderNumber}</p><p>Jami: #{totalAmount}</p>',true),
('22222222-2222-2222-2222-222222222222','ORDER_SHIPPED','EMAIL','Buyurtmangiz #{orderNumber} jo''natildi','<p>Buyurtma #{orderNumber} jo''natildi.</p>',true),
('33333333-3333-3333-3333-333333333333','PAYMENT_SUCCESS','EMAIL','To''lov muvaffaqiyatli','<p>To''lov muvaffaqiyatli amalga oshirildi. Buyurtma: #{orderNumber}</p>',true),
('44444444-4444-4444-4444-444444444444','LOW_STOCK_ALERT','EMAIL','Low stock alert: #{productName}','<p>Mahsulot #{productName} kam qoldi.</p>',true),
('55555555-5555-5555-5555-555555555555','PAYMENT_FAILED','SMS','To''lov xatosi','To''lov xato bo''ldi. Buyurtma: #{orderNumber}',true);
