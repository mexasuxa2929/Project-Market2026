-- V6: permissions jadvali va role kengaytmasi

-- 1. permissions jadvali
CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    category    VARCHAR(100),
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. roles jadvaliga qo'shimcha ustunlar
ALTER TABLE roles ADD COLUMN IF NOT EXISTS display_name  VARCHAR(200);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS description   TEXT;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system     BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS created_at    TIMESTAMP;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMP;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS created_by    VARCHAR(255);

-- 3. role_permissions join jadvali
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 4. Permissions insert (ON CONFLICT DO NOTHING)
-- OMBOR
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'WAREHOUSE_VIEW',          'Ombor ko''rish',         'OMBOR'),
    (gen_random_uuid(), 'WAREHOUSE_MANAGE',        'Ombor boshqarish',       'OMBOR'),
    (gen_random_uuid(), 'STOCK_VIEW',              'Zaxira ko''rish',        'OMBOR'),
    (gen_random_uuid(), 'STOCK_MANAGE',            'Zaxira boshqarish',      'OMBOR'),
    (gen_random_uuid(), 'DELIVERY_VIEW',           'Yetkazish ko''rish',     'OMBOR'),
    (gen_random_uuid(), 'DELIVERY_MANAGE',         'Yetkazish boshqarish',   'OMBOR'),
    (gen_random_uuid(), 'AUDIT_LOG_VIEW',          'Audit log ko''rish',     'OMBOR'),
    (gen_random_uuid(), 'AUDIT_LOG_EXPORT',        'Audit log eksport',      'OMBOR')
ON CONFLICT (name) DO NOTHING;

-- MAHSULOT
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'PRODUCT_VIEW',            'Mahsulot ko''rish',      'MAHSULOT'),
    (gen_random_uuid(), 'PRODUCT_CREATE',          'Mahsulot yaratish',      'MAHSULOT'),
    (gen_random_uuid(), 'PRODUCT_EDIT',            'Mahsulot tahrirlash',    'MAHSULOT'),
    (gen_random_uuid(), 'PRODUCT_DELETE',          'Mahsulot o''chirish',    'MAHSULOT'),
    (gen_random_uuid(), 'PRODUCT_MANAGE',          'Mahsulot boshqarish',    'MAHSULOT'),
    (gen_random_uuid(), 'ORDER_VIEW',              'Buyurtma ko''rish',      'MAHSULOT')
ON CONFLICT (name) DO NOTHING;

-- BUYURTMA
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'ORDER_CREATE',            'Buyurtma yaratish',      'BUYURTMA'),
    (gen_random_uuid(), 'ORDER_EDIT',              'Buyurtma tahrirlash',    'BUYURTMA'),
    (gen_random_uuid(), 'ORDER_DELETE',            'Buyurtma o''chirish',    'BUYURTMA'),
    (gen_random_uuid(), 'ORDER_MANAGE',            'Buyurtma boshqarish',    'BUYURTMA'),
    (gen_random_uuid(), 'NOTIFICATION_VIEW',       'Bildirishnoma ko''rish', 'BUYURTMA'),
    (gen_random_uuid(), 'NOTIFICATION_MANAGE',     'Bildirishnoma boshqarish','BUYURTMA')
ON CONFLICT (name) DO NOTHING;

-- TOLOV
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'PAYMENT_VIEW',            'To''lov ko''rish',       'TOLOV'),
    (gen_random_uuid(), 'PAYMENT_PROCESS',         'To''lovni qayta ishlash','TOLOV'),
    (gen_random_uuid(), 'PAYMENT_REFUND',          'To''lovni qaytarish',    'TOLOV'),
    (gen_random_uuid(), 'PAYMENT_REPORT',          'To''lov hisoboti',       'TOLOV')
ON CONFLICT (name) DO NOTHING;

-- FOYDALANUVCHI
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'USER_VIEW',               'Foydalanuvchi ko''rish',     'FOYDALANUVCHI'),
    (gen_random_uuid(), 'USER_CREATE',             'Foydalanuvchi yaratish',     'FOYDALANUVCHI'),
    (gen_random_uuid(), 'USER_EDIT',               'Foydalanuvchi tahrirlash',   'FOYDALANUVCHI'),
    (gen_random_uuid(), 'USER_DELETE',             'Foydalanuvchi o''chirish',   'FOYDALANUVCHI'),
    (gen_random_uuid(), 'USER_ROLE_ASSIGN',        'Foydalanuvchiga rol berish', 'FOYDALANUVCHI')
ON CONFLICT (name) DO NOTHING;

-- HISOBOT
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'REPORT_VIEW',             'Hisobot ko''rish',       'HISOBOT'),
    (gen_random_uuid(), 'REPORT_EXPORT',           'Hisobot eksport',        'HISOBOT'),
    (gen_random_uuid(), 'REPORT_FINANCIAL',        'Moliyaviy hisobot',      'HISOBOT'),
    (gen_random_uuid(), 'REPORT_WAREHOUSE',        'Ombor hisoboti',         'HISOBOT')
ON CONFLICT (name) DO NOTHING;

-- KURYER
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'COURIER_VIEW',            'Kuryer ko''rish',            'KURYER'),
    (gen_random_uuid(), 'COURIER_MANAGE',          'Kuryer boshqarish',          'KURYER'),
    (gen_random_uuid(), 'DELIVERY_ASSIGN',         'Yetkazishni belgilash',      'KURYER'),
    (gen_random_uuid(), 'DELIVERY_STATUS_UPDATE',  'Yetkazish holatini yangilash','KURYER')
ON CONFLICT (name) DO NOTHING;

-- TIZIM
INSERT INTO permissions (id, name, display_name, category) VALUES
    (gen_random_uuid(), 'SYSTEM_CONFIG',           'Tizim sozlamalari',      'TIZIM'),
    (gen_random_uuid(), 'ROLE_MANAGE',             'Rollarni boshqarish',    'TIZIM'),
    (gen_random_uuid(), 'BACKUP_RESTORE',          'Backup va tiklash',      'TIZIM')
ON CONFLICT (name) DO NOTHING;

-- 5. Mavjud system rollarni belgilash
UPDATE roles SET is_system = TRUE WHERE name IN ('ROLE_SUPER_ADMIN', 'ROLE_ADMIN');

-- 6. ROLE_SUPER_ADMIN ga barcha permissionlarni berish
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN'
ON CONFLICT DO NOTHING;
