-- Variantlar tizimi olib tashlandi: endi har rang mustaqil Product (productId) hisoblanadi.
-- order_item.product_id allaqachon aynan shu (rangga xos) mahsulotni bildiradi,
-- shuning uchun alohida variant_id/variant_name ustunlariga ehtiyoj qolmadi.
-- Tarixiy buyurtmalar uchun ma'lumot yo'qotilmaydi: product_id/product_name ustunlari
-- o'zgarishsiz qoladi, faqat endi "variant" emas, balki aniq rangli mahsulotni bildiradi.

ALTER TABLE order_item DROP COLUMN IF EXISTS variant_id;
ALTER TABLE order_item DROP COLUMN IF EXISTS variant_name;
