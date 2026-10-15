-- Sotuv paytidagi haqiqiy sotuv narxi snapshot'i.
-- Oldin ProfitReport tushumni joriy basePrice × sotilgan miqdor deb hisoblagan,
-- shuning uchun narx o'zgarganda o'tgan davr foydasi buzilib ko'ringan.
-- Endi salesOut harakati paytida narx shu yerda muzlatiladi (unit_price,
-- total_revenue); eski (null) yozuvlar uchun hisobot basePrice fallback beradi.
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS unit_price NUMERIC(19,6);
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS total_revenue NUMERIC(19,6);
