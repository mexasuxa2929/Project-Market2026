-- Manzil modeli: viloyat/tuman/line1/postal matn aralashmasi olib tashlandi.
-- Yangi model: id, name (tanlangan manzil matni), lat, lng + ixtiyoriy line2/phone.
ALTER TABLE shop_customer_address DROP COLUMN IF EXISTS region;
ALTER TABLE shop_customer_address DROP COLUMN IF EXISTS district;
ALTER TABLE shop_customer_address DROP COLUMN IF EXISTS line1;
ALTER TABLE shop_customer_address DROP COLUMN IF EXISTS postal_code;
ALTER TABLE shop_customer_address ADD COLUMN IF NOT EXISTS name VARCHAR(512);
ALTER TABLE shop_customer_address ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE shop_customer_address ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
