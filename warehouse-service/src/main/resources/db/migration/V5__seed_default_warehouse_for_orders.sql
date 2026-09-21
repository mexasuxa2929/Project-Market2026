-- order-service Feign chaqiruvlari uchun default ombor (app.warehouse.default-id bilan bir xil UUID).
-- Allaqachon boshqa omborlar bo'lsa ham, bu qator faqat ushbu id yo'q bo'lsa qo'shiladi.
INSERT INTO warehouse (id, name, location, address, active)
SELECT '11111111-1111-4111-8111-111111111111'::uuid,
       'Orders integration (seed)',
       'Main',
       'order-service default-id (WAREHOUSE_DEFAULT_ID)',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM warehouse WHERE id = '11111111-1111-4111-8111-111111111111'::uuid
);
