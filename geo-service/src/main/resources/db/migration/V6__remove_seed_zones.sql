-- V1 dagi test seed zonalar o'chirildi (koddan ham olib tashlandi).
-- Avval migratsiya bo'lgan bazalarda shu qatorlarni tozalaydi.
DELETE FROM geo_zone WHERE id IN (
    'a1000000-0000-0000-0000-000000000001',
    'a1000000-0000-0000-0000-000000000002',
    'a1000000-0000-0000-0000-000000000003'
);
