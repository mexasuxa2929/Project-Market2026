-- LOW_STOCK_ALERT shablonini boyitilgan tarkibga yangilash (warehouse-service scheduler orqali).
UPDATE notification_template
SET body_template = 'Salom! #{warehouseName} omborida quyidagi mahsulotlar kam qoldi:\n- #{productName}: #{quantity} dona (min. #{minStock})'
WHERE type = 'LOW_STOCK_ALERT'
  AND channel = 'EMAIL';
