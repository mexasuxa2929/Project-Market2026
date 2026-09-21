-- Category IDs:
-- Avtotovarlar:   f14037af-2162-454d-a45b-2e78f8167294
-- Bog va tomorqa: 570b718c-990c-44e0-957b-78959bf59761
-- Elektronika:    d52a25d9-f37b-468c-b9c2-04a33b2b84aa
-- Gozallik:       ec63c33a-7140-4df0-bd8d-b932ee2ecc3f
-- Ichimliklar:    f759e54c-469f-46c7-b7f3-5db7ba1df004
-- Kameralar:      d5344e6f-1be5-4747-93bb-884bd37f8d2f
-- Kanselyariya:   8c38c710-bc78-41fa-b2a2-1f690a43a2e7
-- Kitoblar:       e165a853-c5e5-44c1-8581-d06a1891740d
-- Kiyim-kechak:   1aa7c897-5e42-4432-9a88-d3dd3ca958ca
-- Mebel:          210596ca-50a5-434a-b7d2-d046c4161de1
-- Oyinchoqlar:    048d953b-3372-403d-8165-1feb7a4244ec
-- Oziq-ovqat:     899da804-63b9-4b4b-bebe-6d7bc7a6d5c5
-- Sport:          447d7b28-70e7-4200-a789-cd07c5bc4c6b
-- Uy hayvonlari:  3b0595fc-392b-4b7c-b94d-48ab1147bae5
-- Uy jihozlari:   750f48b6-badf-4492-a7d6-393c39ad25c7

-- Brand IDs:
-- Adidas:            abc4e0ab-d557-40b5-b930-ab0147ef550a
-- Apple:             e85b6ef2-6a3e-423b-a654-360533bebf08
-- Artel:             9a771ea7-cf15-407c-aebf-b3d483655005
-- Bosch:             ca6ecd9d-225a-4a32-b96c-755c286d3386
-- Boshqalar:         4160f372-556b-4a42-a4ca-8a9699004a50
-- Coca-Cola:         2db4e1d7-e128-4df9-b216-e2bb58f97356
-- LG:                ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b
-- Mahalliy brendlar: 6ab89b9c-9219-493f-aa15-f1011529fa05
-- Nestle:            3fa752a2-3484-4797-8c9a-957481b003c9
-- Panasonic:         d41880af-0be0-46aa-9e3a-dc3934d3213a
-- PepsiCo:           9b2e86a2-b165-437f-97f3-a67384b1e622
-- Samsung:           4a7e55be-ec37-4d30-93ee-63ae66b0ae06
-- Samsung Life:      e17e92d2-798b-4c10-9be0-b64af577c3ed
-- Sony:              3599076a-3729-448b-abe2-29f2c486b217
-- Xiaomi:            063dfdf2-32c3-4723-b732-bd253309685e

-- ===================================================================
-- 1. Elektronika (6 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'iPhone 15 Pro Max',
    '{"uz": "iPhone 15 Pro Max", "ru": "Айфон 15 Про Макс"}',
    'iPhone 15 Pro Max - eng yangi texnologiyalar bilan jihozlangan smartfon. Titan korpus, A17 Pro chip, 48MP kamera, USB-C.',
    '{"uz": "iPhone 15 Pro Max - eng yangi texnologiyalar bilan jihozlangan smartfon. Titan korpus, A17 Pro chip, 48MP kamera, USB-C.", "ru": "iPhone 15 Pro Max — новейший смартфон с передовыми технологиями. Титановый корпус, чип A17 Pro, 48МП камера, USB-C."}',
    'Eng soʻnggi texnologiyali flagship smartfon',
    '{"uz": "Eng soʻnggi texnologiyali flagship smartfon", "ru": "Флагманский смартфон с новейшими технологиями"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', 'e85b6ef2-6a3e-423b-a654-360533bebf08', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Galaxy S24 Ultra',
    '{"uz": "Samsung Galaxy S24 Ultra", "ru": "Самсунг Галакси С24 Ультра"}',
    'Samsung Galaxy S24 Ultra - Galaxy AI bilan jihozlangan premium smartfon. S Pen, 200MP kamera, Dynamic AMOLED 2X ekran.',
    '{"uz": "Samsung Galaxy S24 Ultra - Galaxy AI bilan jihozlangan premium smartfon. S Pen, 200MP kamera, Dynamic AMOLED 2X ekran.", "ru": "Samsung Galaxy S24 Ultra — премиум смартфон с Galaxy AI. S Pen, 200МП камера, Dynamic AMOLED 2X дисплей."}',
    'Galaxy AI bilan premium smartfon',
    '{"uz": "Galaxy AI bilan premium smartfon", "ru": "Премиум смартфон с Galaxy AI"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', '4a7e55be-ec37-4d30-93ee-63ae66b0ae06', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Xiaomi Redmi Note 13 Pro',
    '{"uz": "Xiaomi Redmi Note 13 Pro", "ru": "Сяоми Редми Ноут 13 Про"}',
    'Xiaomi Redmi Note 13 Pro - ajoyib narx-sifat nisbatiga ega smartfon. 200MP kamera, 120Hz AMOLED ekran, 67W tezkor zaryad.',
    '{"uz": "Xiaomi Redmi Note 13 Pro - ajoyib narx-sifat nisbatiga ega smartfon. 200MP kamera, 120Hz AMOLED ekran, 67W tezkor zaryad.", "ru": "Xiaomi Redmi Note 13 Pro — смартфон с отличным соотношением цена-качество. 200МП камера, 120Гц AMOLED экран, быстрая зарядка 67 Вт."}',
    'Ajoyib narx-sifat nisbati',
    '{"uz": "Ajoyib narx-sifat nisbati", "ru": "Отличное соотношение цена-качество"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', '063dfdf2-32c3-4723-b732-bd253309685e', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Sony WH-1000XM5 naushniklari',
    '{"uz": "Sony WH-1000XM5 naushniklari", "ru": "Наушники Sony WH-1000XM5"}',
    'Sony WH-1000XM5 - eng yaxshi shovqin oʻchirish tizimiga ega simsiz naushniklar. 30 soat batareya, Hi-Res Audio, qulay dizayn.',
    '{"uz": "Sony WH-1000XM5 - eng yaxshi shovqin oʻchirish tizimiga ega simsiz naushniklar. 30 soat batareya, Hi-Res Audio, qulay dizayn.", "ru": "Sony WH-1000XM5 — лучшие беспроводные наушники с системой шумоподавления. 30 часов работы, Hi-Res Audio, удобный дизайн."}',
    'Eng yaxshi shovqin oʻchirish tizimi',
    '{"uz": "Eng yaxshi shovqin oʻchirish tizimi", "ru": "Лучшая система шумоподавления"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', '3599076a-3729-448b-abe2-29f2c486b217', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'LG 43" 4K Ultra HD televizor',
    '{"uz": "LG 43 4K Ultra HD televizor", "ru": "Телевизор LG 43 4K Ultra HD"}',
    'LG 43" 4K Ultra HD Smart TV - WebOS 23, ThinQ AI, HDR10 Pro, Dolby Audio, simsiz ulanish.',
    '{"uz": "LG 43 4K Ultra HD Smart TV - WebOS 23, ThinQ AI, HDR10 Pro, Dolby Audio, simsiz ulanish.", "ru": "LG 43 4K Ultra HD Smart TV — WebOS 23, ThinQ AI, HDR10 Pro, Dolby Audio, беспроводное подключение."}',
    '4K Ultra HD Smart TV',
    '{"uz": "4K Ultra HD Smart TV", "ru": "4K Ultra HD Smart TV"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Apple MacBook Air M3',
    '{"uz": "Apple MacBook Air M3", "ru": "Эпл Макбук Эйр М3"}',
    'Apple MacBook Air M3 - 13.6" Liquid Retina displey, Apple M3 chip, 18 soat batareya, 8GB/256GB, yengil va nafis dizayn.',
    '{"uz": "Apple MacBook Air M3 - 13.6 dyumli Liquid Retina displey, Apple M3 chip, 18 soat batareya, 8GB/256GB, yengil va nafis dizayn.", "ru": "Apple MacBook Air M3 — 13.6 дюймов Liquid Retina дисплей, чип Apple M3, 18 часов работы, 8ГБ/256ГБ, легкий и элегантный дизайн."}',
    'Yengil va qudratli noutbuk',
    '{"uz": "Yengil va qudratli noutbuk", "ru": "Легкий и мощный ноутбук"}',
    'dona', 'd52a25d9-f37b-468c-b9c2-04a33b2b84aa', 'e85b6ef2-6a3e-423b-a654-360533bebf08', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 2. Uy jihozlari (6 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bosch kir yuvish mashinasi',
    '{"uz": "Bosch kir yuvish mashinasi", "ru": "Стиральная машина Bosch"}',
    'Bosch Series 6 kir yuvish mashinasi - 8kg, 1400 aylanish, EcoSilence Drive, ActiveWater, VarioPerfect.',
    '{"uz": "Bosch Series 6 kir yuvish mashinasi - 8kg, 1400 aylanish, EcoSilence Drive, ActiveWater, VarioPerfect.", "ru": "Стиральная машина Bosch Series 6 — 8кг, 1400 оборотов, EcoSilence Drive, ActiveWater, VarioPerfect."}',
    'Ishonchli va tejamkor kir yuvish',
    '{"uz": "Ishonchli va tejamkor kir yuvish", "ru": "Надежная и экономичная стирка"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', 'ca6ecd9d-225a-4a32-b96c-755c286d3386', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel muzlatgichi',
    '{"uz": "Artel muzlatgichi", "ru": "Холодильник Artel"}',
    'Artel ikki kamerali muzlatgich - 350L, No Frost, Energy A+, LED yoritish, tez sovutish rejimi.',
    '{"uz": "Artel ikki kamerali muzlatgich - 350L, No Frost, Energy A+, LED yoritish, tez sovutish rejimi.", "ru": "Двухкамерный холодильник Artel — 350Л, No Frost, Energy A+, LED подсветка, режим быстрого охлаждения."}',
    'Mahalliy ishlab chiqaruvchidan sifatli muzlatgich',
    '{"uz": "Mahalliy ishlab chiqaruvchidan sifatli muzlatgich", "ru": "Качественный холодильник от местного производителя"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung mikrodolqinli pech',
    '{"uz": "Samsung mikrodolqinli pech", "ru": "Микроволновая печь Samsung"}',
    'Samsung mikrodolqinli pech - 28L, 900W, keramik qoplama, Eco Mode, grill funktsiyasi.',
    '{"uz": "Samsung mikrodolqinli pech - 28L, 900W, keramik qoplama, Eco Mode, grill funktsiyasi.", "ru": "Микроволновая печь Samsung — 28Л, 900Вт, керамическое покрытие, Eco Mode, функция гриля."}',
    'Zamonaviy mikrotoʻlqinli pech',
    '{"uz": "Zamonaviy mikrotoʻlqinli pech", "ru": "Современная микроволновая печь"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', '4a7e55be-ec37-4d30-93ee-63ae66b0ae06', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Panasonic changyutkichi',
    '{"uz": "Panasonic changyutkichi", "ru": "Пылесос Panasonic"}',
    'Panasonic simsiz changyutkich - 450W, siklon filtr, 45 daqiqa ishlash, 2L chang idishi.',
    '{"uz": "Panasonic simsiz changyutkich - 450W, siklon filtr, 45 daqiqa ishlash, 2L chang idishi.", "ru": "Беспроводной пылесос Panasonic — 450Вт, циклонный фильтр, 45 минут работы, 2Л контейнер."}',
    'Simsiz va kuchli changyutkich',
    '{"uz": "Simsiz va kuchli changyutkich", "ru": "Беспроводной и мощный пылесос"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', 'd41880af-0be0-46aa-9e3a-dc3934d3213a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'LG konditsioneri',
    '{"uz": "LG konditsioneri", "ru": "Кондиционер LG"}',
    'LG Split konditsioner - 12000 BTU, Inverter, Wi-Fi boshqaruv, ionli tozalash, Dual Cool.',
    '{"uz": "LG Split konditsioner - 12000 BTU, Inverter, Wi-Fi boshqaruv, ionli tozalash, Dual Cool.", "ru": "Сплит-кондиционер LG — 12000 BTU, инвертор, управление по Wi-Fi, ионная очистка, Dual Cool."}',
    'Inverter konditsioner yuqori samaradorlik',
    '{"uz": "Inverter konditsioner yuqori samaradorlik", "ru": "Инверторный кондиционер с высокой эффективностью"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel elektr choynak',
    '{"uz": "Artel elektr choynak", "ru": "Электрочайник Artel"}',
    'Artel elektr choynak - 1.7L, 2200W, zanglamaydigan poʻlat, avtomatik oʻchish, aylanuvchi asos.',
    '{"uz": "Artel elektr choynak - 1.7L, 2200W, zanglamaydigan poʻlat, avtomatik oʻchish, aylanuvchi asos.", "ru": "Электрочайник Artel — 1.7Л, 2200Вт, нержавеющая сталь, автоотключение, поворотная подставка."}',
    'Tez qaynaydigan elektr choynak',
    '{"uz": "Tez qaynaydigan elektr choynak", "ru": "Быстрозакипающий электрочайник"}',
    'dona', '750f48b6-badf-4492-a7d6-393c39ad25c7', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 3. Kiyim-kechak (5 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Adidas erkaklar sport krossovkalari',
    '{"uz": "Adidas erkaklar sport krossovkalari", "ru": "Кроссовки спортивные мужские Adidas"}',
    'Adidas erkaklar sport krossovkalari - yengil, nafas oluvchi toʻr mato, Cloudfoam taglik, zamonaviy dizayn.',
    '{"uz": "Adidas erkaklar sport krossovkalari - yengil, nafas oluvchi toʻr mato, Cloudfoam taglik, zamonaviy dizayn.", "ru": "Мужские спортивные кроссовки Adidas — легкие, дышащая сетка, подошва Cloudfoam, современный дизайн."}',
    'Qulay va zamonaviy sport krossovkalari',
    '{"uz": "Qulay va zamonaviy sport krossovkalari", "ru": "Удобные и стильные спортивные кроссовки"}',
    'dona', '1aa7c897-5e42-4432-9a88-d3dd3ca958ca', 'abc4e0ab-d557-40b5-b930-ab0147ef550a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Erkaklar futbolkasi (paxta)',
    '{"uz": "Erkaklar futbolkasi (paxta)", "ru": "Мужская футболка (хлопок)"}',
    'Erkaklar paxta futbolkasi - 100% paxta, yumshoq, nafas oluvchi, turli ranglar mavjud.',
    '{"uz": "Erkaklar paxta futbolkasi - 100% paxta, yumshoq, nafas oluvchi, turli ranglar mavjud.", "ru": "Мужская хлопковая футболка — 100% хлопок, мягкая, дышащая, доступна в разных цветах."}',
    'Tabiiy paxtadan tayyorlangan futbolka',
    '{"uz": "Tabiiy paxtadan tayyorlangan futbolka", "ru": "Футболка из натурального хлопка"}',
    'dona', '1aa7c897-5e42-4432-9a88-d3dd3ca958ca', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Ayollar paltosi',
    '{"uz": "Ayollar paltosi", "ru": "Женское пальто"}',
    'Ayollar demi-sezon paltosi - yuqori sifatli mato, klassik kesim, ikki tomonlama, oʻrta uzunlik.',
    '{"uz": "Ayollar demi-sezon paltosi - yuqori sifatli mato, klassik kesim, ikki tomonlama, oʻrta uzunlik.", "ru": "Демисезонное женское пальто — высококачественная ткань, классический крой, двустороннее, средней длины."}',
    'Nafis va issiq ayollar paltosi',
    '{"uz": "Nafis va issiq ayollar paltosi", "ru": "Элегантное и теплое женское пальто"}',
    'dona', '1aa7c897-5e42-4432-9a88-d3dd3ca958ca', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Adidas erkaklar shimi',
    '{"uz": "Adidas erkaklar shimi", "ru": "Мужские штаны Adidas"}',
    'Adidas erkaklar sport shimi - yumshoq trikotaj, elastik belbog, manjetlar, katta choʻntaklar.',
    '{"uz": "Adidas erkaklar sport shimi - yumshoq trikotaj, elastik belbog, manjetlar, katta choʻntaklar.", "ru": "Мужские спортивные штаны Adidas — мягкий трикотаж, эластичный пояс, манжеты, большие карманы."}',
    'Qulay sport shimi',
    '{"uz": "Qulay sport shimi", "ru": "Удобные спортивные штаны"}',
    'dona', '1aa7c897-5e42-4432-9a88-d3dd3ca958ca', 'abc4e0ab-d557-40b5-b930-ab0147ef550a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bolalar kurtkasi',
    '{"uz": "Bolalar kurtkasi", "ru": "Детская куртка"}',
    'Bolalar kurtkasi - suv oʻtkazmaydigan, yengil, shamoldan himoya, olinadigan kapyushon.',
    '{"uz": "Bolalar kurtkasi - suv oʻtkazmaydigan, yengil, shamoldan himoya, olinadigan kapyushon.", "ru": "Детская куртка — водонепроницаемая, легкая, защита от ветра, съемный капюшон."}',
    'Suv oʻtkazmaydigan bolalar kurtkasi',
    '{"uz": "Suv oʻtkazmaydigan bolalar kurtkasi", "ru": "Водонепроницаемая детская куртка"}',
    'dona', '1aa7c897-5e42-4432-9a88-d3dd3ca958ca', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 4. Oziq-ovqat (5 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Nestle sutli shokolad',
    '{"uz": "Nestle sutli shokolad", "ru": "Молочный шоколад Nestle"}',
    'Nestle sutli shokolad - 100g, yuqori sifatli kakao, tabiiy sut, lazzatli va yumshoq.',
    '{"uz": "Nestle sutli shokolad - 100g, yuqori sifatli kakao, tabiiy sut, lazzatli va yumshoq.", "ru": "Молочный шоколад Nestle — 100г, высококачественное какао, натуральное молоко, вкусный и нежный."}',
    'Sevimli sutli shokolad',
    '{"uz": "Sevimli sutli shokolad", "ru": "Любимый молочный шоколад"}',
    'dona', '899da804-63b9-4b4b-bebe-6d7bc7a6d5c5', '3fa752a2-3484-4797-8c9a-957481b003c9', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Nestle kofe 3v1',
    '{"uz": "Nestle kofe 3v1", "ru": "Кофе 3в1 Nestle"}',
    'Nestle 3v1 kofe paketi - 20 paketcha, tez tayyorlanadi, mukammal taʼm va boy kofe aromati.',
    '{"uz": "Nestle 3v1 kofe paketi - 20 paketcha, tez tayyorlanadi, mukammal taʼm va boy kofe aromati.", "ru": "Кофе 3в1 Nestle — 20 пакетиков, быстрого приготовления, отличный вкус и богатый кофейный аромат."}',
    'Tez tayyorlanadigan kofe',
    '{"uz": "Tez tayyorlanadigan kofe", "ru": "Кофе быстрого приготовления"}',
    'dona', '899da804-63b9-4b4b-bebe-6d7bc7a6d5c5', '3fa752a2-3484-4797-8c9a-957481b003c9', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy asal (1L)',
    '{"uz": "Mahalliy asal 1L", "ru": "Местный мед 1Л"}',
    'Tabiiy togʻ asali - 1L, ekologik toza, shakarsiz, bevosita asalarichilardan.',
    '{"uz": "Tabiiy togʻ asali - 1L, ekologik toza, shakarsiz, bevosita asalarichilardan.", "ru": "Натуральный горный мед — 1Л, экологически чистый, без сахара, напрямую от пчеловодов."}',
    'Tabiiy togʻ asali',
    '{"uz": "Tabiiy togʻ asali", "ru": "Натуральный горный мед"}',
    'dona', '899da804-63b9-4b4b-bebe-6d7bc7a6d5c5', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy yogʻ (500g)',
    '{"uz": "Mahalliy yog 500g", "ru": "Местное сливочное масло 500г"}',
    'Tabiiy sariq yogʻ - 500g, qaymoqdan tayyorlangan, yuqori yogʻlilik, konservantsiz.',
    '{"uz": "Tabiiy sariq yogʻ - 500g, qaymoqdan tayyorlangan, yuqori yogʻlilik, konservantsiz.", "ru": "Натуральное сливочное масло — 500г, изготовлено из сливок, высокая жирность, без консервантов."}',
    'Tabiiy sariq yogʻ',
    '{"uz": "Tabiiy sariq yogʻ", "ru": "Натуральное сливочное масло"}',
    'dona', '899da804-63b9-4b4b-bebe-6d7bc7a6d5c5', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Nestle chaqaloq ovqati',
    '{"uz": "Nestle chaqaloq ovqati", "ru": "Детское питание Nestle"}',
    'Nestle chaqaloq ovqati - 6 oydan boshlab, vitaminlar kompleksi, tabiiy ingredientlar, allergensiz.',
    '{"uz": "Nestle chaqaloq ovqati - 6 oydan boshlab, vitaminlar kompleksi, tabiiy ingredientlar, allergensiz.", "ru": "Детское питание Nestle — с 6 месяцев, комплекс витаминов, натуральные ингредиенты, без аллергенов."}',
    'Sogʻlom chaqaloq ovqati',
    '{"uz": "Sogʻlom chaqaloq ovqati", "ru": "Здоровое детское питание"}',
    'dona', '899da804-63b9-4b4b-bebe-6d7bc7a6d5c5', '3fa752a2-3484-4797-8c9a-957481b003c9', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 5. Ichimliklar (4 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Coca-Cola 1.5L',
    '{"uz": "Coca-Cola 1.5L", "ru": "Кока-Кола 1.5Л"}',
    'Mashhur karbonatli ichimlik Coca-Cola - 1.5L shisha, tetiklantiruvchi taʼm, har qanday bayram uchun ideal.',
    '{"uz": "Mashhur karbonatli ichimlik Coca-Cola - 1.5L shisha, tetiklantiruvchi taʼm, har qanday bayram uchun ideal.", "ru": "Известный газированный напиток Coca-Cola — 1.5Л бутылка, освежающий вкус, идеален для любого праздника."}',
    'Tetiklantiruvchi Coca-Cola',
    '{"uz": "Tetiklantiruvchi Coca-Cola", "ru": "Освежающая Кока-Кола"}',
    'dona', 'f759e54c-469f-46c7-b7f3-5db7ba1df004', '2db4e1d7-e128-4df9-b216-e2bb58f97356', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Pepsi 1L',
    '{"uz": "Pepsi 1L", "ru": "Пепси 1Л"}',
    'Pepsi karbonatli ichimlik - 1L shisha, boy taʼm, mukammal tetiklantiruvchi ichimlik.',
    '{"uz": "Pepsi karbonatli ichimlik - 1L shisha, boy taʼm, mukammal tetiklantiruvchi ichimlik.", "ru": "Газированный напиток Pepsi — 1Л бутылка, богатый вкус, идеально освежает."}',
    'Mashhur Pepsi ichimligi',
    '{"uz": "Mashhur Pepsi ichimligi", "ru": "Известный напиток Pepsi"}',
    'dona', 'f759e54c-469f-46c7-b7f3-5db7ba1df004', '9b2e86a2-b165-437f-97f3-a67384b1e622', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Coca-Cola Zero 0.5L',
    '{"uz": "Coca-Cola Zero 0.5L", "ru": "Кока-Кола Зеро 0.5Л"}',
    'Coca-Cola Zero shakarsiz - 0.5L, bir xil ajoyib taʼm, ammo shakarsiz va kaloriyasiz.',
    '{"uz": "Coca-Cola Zero shakarsiz - 0.5L, bir xil ajoyib taʼm, ammo shakarsiz va kaloriyasiz.", "ru": "Coca-Cola Zero без сахара — 0.5Л, тот же отличный вкус, но без сахара и калорий."}',
    'Shakarsiz Coca-Cola',
    '{"uz": "Shakarsiz Coca-Cola", "ru": "Coca-Cola без сахара"}',
    'dona', 'f759e54c-469f-46c7-b7f3-5db7ba1df004', '2db4e1d7-e128-4df9-b216-e2bb58f97356', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'PepsiCo Lays chipslari',
    '{"uz": "PepsiCo Lays chipslari", "ru": "Чипсы Lays от PepsiCo"}',
    'Lays chipslari - 150g, tayyor kartoshkadan, xushboʻy, qarsildoq, sevimli gazak.',
    '{"uz": "Lays chipslari - 150g, tayyor kartoshkadan, xushboʻy, qarsildoq, sevimli gazak.", "ru": "Чипсы Lays — 150г, из отборного картофеля, ароматные, хрустящие, любимый снек."}',
    'Xushboʻy chipslar',
    '{"uz": "Xushboʻy chipslar", "ru": "Ароматные чипсы"}',
    'dona', 'f759e54c-469f-46c7-b7f3-5db7ba1df004', '9b2e86a2-b165-437f-97f3-a67384b1e622', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 6. Go'zallik (4 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Life tish pastasi',
    '{"uz": "Samsung Life tish pastasi", "ru": "Зубная паста Samsung Life"}',
    'Samsung Life tish pastasi - 100ml, ftorli, oqartiruvchi, tish emalini mustahkamlaydi, yangi nafas.',
    '{"uz": "Samsung Life tish pastasi - 100ml, ftorli, oqartiruvchi, tish emalini mustahkamlaydi, yangi nafas.", "ru": "Зубная паста Samsung Life — 100мл, с фтором, отбеливающая, укрепляет эмаль, свежее дыхание."}',
    'Oqartiruvchi tish pastasi',
    '{"uz": "Oqartiruvchi tish pastasi", "ru": "Отбеливающая зубная паста"}',
    'dona', 'ec63c33a-7140-4df0-bd8d-b932ee2ecc3f', 'e17e92d2-798b-4c10-9be0-b64af577c3ed', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Life sovuni',
    '{"uz": "Samsung Life sovuni", "ru": "Мыло Samsung Life"}',
    'Samsung Life kir sovuni - 200g, antibakterial, namlantiruvchi, tabiiy ingredientlar, qoʻl va yuz uchun.',
    '{"uz": "Samsung Life kir sovuni - 200g, antibakterial, namlantiruvchi, tabiiy ingredientlar, qoʻl va yuz uchun.", "ru": "Туалетное мыло Samsung Life — 200г, антибактериальное, увлажняющее, натуральные ингредиенты, для рук и лица."}',
    'Antibakterial sovun',
    '{"uz": "Antibakterial sovun", "ru": "Антибактериальное мыло"}',
    'dona', 'ec63c33a-7140-4df0-bd8d-b932ee2ecc3f', 'e17e92d2-798b-4c10-9be0-b64af577c3ed', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Life shampun',
    '{"uz": "Samsung Life shampun", "ru": "Шампунь Samsung Life"}',
    'Samsung Life shampun - 400ml, namlantiruvchi, barcha soch turlari uchun, keratin bilan.',
    '{"uz": "Samsung Life shampun - 400ml, namlantiruvchi, barcha soch turlari uchun, keratin bilan.", "ru": "Шампунь Samsung Life — 400мл, увлажняющий, для всех типов волос, с кератином."}',
    'Namlantiruvchi shampun',
    '{"uz": "Namlantiruvchi shampun", "ru": "Увлажняющий шампунь"}',
    'dona', 'ec63c33a-7140-4df0-bd8d-b932ee2ecc3f', 'e17e92d2-798b-4c10-9be0-b64af577c3ed', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy efir moyi',
    '{"uz": "Mahalliy efir moyi", "ru": "Местное эфирное масло"}',
    'Tabiiy efir moyi - 10ml, lavanda aromati, stressga qarshi, uyqu sifatini yaxshilash uchun.',
    '{"uz": "Tabiiy efir moyi - 10ml, lavanda aromati, stressga qarshi, uyqu sifatini yaxshilash uchun.", "ru": "Натуральное эфирное масло — 10мл, аромат лаванды, антистресс, улучшение качества сна."}',
    'Tabiiy lavanda moyi',
    '{"uz": "Tabiiy lavanda moyi", "ru": "Натуральное лавандовое масло"}',
    'dona', 'ec63c33a-7140-4df0-bd8d-b932ee2ecc3f', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 7. Sport tovarlari (4 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Adidas futbol toʻpi',
    '{"uz": "Adidas futbol topi", "ru": "Футбольный мяч Adidas"}',
    'Adidas futbol toʻpi - 5-oʻlcham, suv oʻtkazmaydigan, yuqori sifatli teri, FIFA sertifikatlangan.',
    '{"uz": "Adidas futbol toʻpi - 5-oʻlcham, suv oʻtkazmaydigan, yuqori sifatli teri, FIFA sertifikatlangan.", "ru": "Футбольный мяч Adidas — размер 5, водонепроницаемый, высококачественная кожа, сертифицирован FIFA."}',
    'Professional futbol toʻpi',
    '{"uz": "Professional futbol topi", "ru": "Профессиональный футбольный мяч"}',
    'dona', '447d7b28-70e7-4200-a789-cd07c5bc4c6b', 'abc4e0ab-d557-40b5-b930-ab0147ef550a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Adidas gimnastika toʻshagi',
    '{"uz": "Adidas gimnastika toshagi", "ru": "Гимнастический коврик Adidas"}',
    'Adidas yoga va gimnastika toʻshagi - 6mm qalinlik, slip-gʻayri, yengil, tashish uchun tasmali.',
    '{"uz": "Adidas yoga va gimnastika toʻshagi - 6mm qalinlik, slip-gʻayri, yengil, tashish uchun tasmali.", "ru": "Коврик для йоги и гимнастики Adidas — 6мм толщина, нескользящий, легкий, с ремнем для переноски."}',
    'Yoga va gimnastika uchun kovrik',
    '{"uz": "Yoga va gimnastika uchun kovrik", "ru": "Коврик для йоги и гимнастики"}',
    'dona', '447d7b28-70e7-4200-a789-cd07c5bc4c6b', 'abc4e0ab-d557-40b5-b930-ab0147ef550a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy gantel toʻplami',
    '{"uz": "Mahalliy gantel toplami", "ru": "Набор гантелей местный"}',
    'Gantel toʻplami - 2kg, 4kg, 6kg, 8kg (4 dona), temir quyma, rezina qoplama, uy sporti uchun ideal.',
    '{"uz": "Gantel toʻplami - 2kg, 4kg, 6kg, 8kg (4 dona), temir quyma, rezina qoplama, uy sporti uchun ideal.", "ru": "Набор гантелей — 2кг, 4кг, 6кг, 8кг (4 шт), литой металл, резиновое покрытие, идеально для дома."}',
    'Uy sporti uchun gantellar',
    '{"uz": "Uy sporti uchun gantellar", "ru": "Гантели для домашних тренировок"}',
    'dona', '447d7b28-70e7-4200-a789-cd07c5bc4c6b', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Life termosi',
    '{"uz": "Samsung Life termosi", "ru": "Термос Samsung Life"}',
    'Samsung Life termosi - 1L, zanglamaydigan poʻlat, 12 soat issiq / 24 soat sovuq saqlaydi.',
    '{"uz": "Samsung Life termosi - 1L, zanglamaydigan poʻlat, 12 soat issiq / 24 soat sovuq saqlaydi.", "ru": "Термос Samsung Life — 1Л, нержавеющая сталь, сохраняет тепло 12 часов / холод 24 часа."}',
    'Issiq va sovuqni uzoq saqlovchi termos',
    '{"uz": "Issiq va sovuqni uzoq saqlovchi termos", "ru": "Термос, долго сохраняющий тепло и холод"}',
    'dona', '447d7b28-70e7-4200-a789-cd07c5bc4c6b', 'e17e92d2-798b-4c10-9be0-b64af577c3ed', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 8. Avtotovarlar (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bosch avtomobil akkumulyatori',
    '{"uz": "Bosch avtomobil akkumulyatori", "ru": "Автомобильный аккумулятор Bosch"}',
    'Bosch avtomobil akkumulyatori - 60Ah, 540A, quruq zaryadlangan, ishonchli start, uzoq xizmat muddati.',
    '{"uz": "Bosch avtomobil akkumulyatori - 60Ah, 540A, quruq zaryadlangan, ishonchli start, uzoq xizmat muddati.", "ru": "Автомобильный аккумулятор Bosch — 60Ач, 540А, сухозаряженный, надежный запуск, долгий срок службы."}',
    'Ishonchli avto akkumulyator',
    '{"uz": "Ishonchli avto akkumulyator", "ru": "Надежный авто аккумулятор"}',
    'dona', 'f14037af-2162-454d-a45b-2e78f8167294', 'ca6ecd9d-225a-4a32-b96c-755c286d3386', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bosch avtomobil moyi',
    '{"uz": "Bosch avtomobil moyi", "ru": "Моторное масло Bosch"}',
    'Bosch motor moyi - 5W-40, 4L, sintetik, benzin va dizel dvigatellar uchun, yuqori himoya.',
    '{"uz": "Bosch motor moyi - 5W-40, 4L, sintetik, benzin va dizel dvigatellar uchun, yuqori himoya.", "ru": "Моторное масло Bosch — 5W-40, 4Л, синтетика, для бензиновых и дизельных двигателей, высокая защита."}',
    'Sintetik motor moyi',
    '{"uz": "Sintetik motor moyi", "ru": "Синтетическое моторное масло"}',
    'dona', 'f14037af-2162-454d-a45b-2e78f8167294', 'ca6ecd9d-225a-4a32-b96c-755c286d3386', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel avtomobil changyutkichi',
    '{"uz": "Artel avtomobil changyutkichi", "ru": "Автомобильный пылесос Artel"}',
    'Artel avtomobil changyutkichi - 12V, 80W, sigaret zajigalkasiga ulanadi, portativ, filtrli.',
    '{"uz": "Artel avtomobil changyutkichi - 12V, 80W, sigaret zajigalkasiga ulanadi, portativ, filtrli.", "ru": "Автомобильный пылесос Artel — 12В, 80Вт, подключается к прикуривателю, портативный, с фильтром."}',
    'Portativ avto changyutkich',
    '{"uz": "Portativ avto changyutkich", "ru": "Портативный авто пылесос"}',
    'dona', 'f14037af-2162-454d-a45b-2e78f8167294', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 9. Mebel (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel oshxona stoli',
    '{"uz": "Artel oshxona stoli", "ru": "Кухонный стол Artel"}',
    'Artel oshxona stoli - 120x80 sm, yogʻoch taxta, metall oyoqlar, zamonaviy dizayn, 6 kishilik.',
    '{"uz": "Artel oshxona stoli - 120x80 sm, yogʻoch taxta, metall oyoqlar, zamonaviy dizayn, 6 kishilik.", "ru": "Кухонный стол Artel — 120x80 см, деревянная столешница, металлические ножки, современный дизайн, на 6 персон."}',
    'Oshxona uchun zamonaviy stol',
    '{"uz": "Oshxona uchun zamonaviy stol", "ru": "Современный стол для кухни"}',
    'dona', '210596ca-50a5-434a-b7d2-d046c4161de1', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy yumshoq divan',
    '{"uz": "Mahalliy yumshoq divan", "ru": "Мягкий диван местный"}',
    'Yumshoq divan - 3 oʻrinli, yuqori sifatli mato, koʻpik toʻldiruvchi, yigʻiladigan mexanizm.',
    '{"uz": "Yumshoq divan - 3 oʻrinli, yuqori sifatli mato, koʻpik toʻldiruvchi, yigʻiladigan mexanizm.", "ru": "Мягкий диван — 3-местный, высококачественная ткань, поролоновый наполнитель, раскладной механизм."}',
    '3 oʻrinli yumshoq divan',
    '{"uz": "3 oʻrinli yumshoq divan", "ru": "3-местный мягкий диван"}',
    'dona', '210596ca-50a5-434a-b7d2-d046c4161de1', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy shkaf',
    '{"uz": "Mahalliy shkaf", "ru": "Шкаф местный"}',
    'Kiyim shkafi - 3 eshik, laminatlangan DSP, kuzgulu, novcha, keng ichki boʻlimlar.',
    '{"uz": "Kiyim shkafi - 3 eshik, laminatlangan DSP, kuzgulu, novcha, keng ichki boʻlimlar.", "ru": "Шкаф для одежды — 3 дверцы, ламинированное ДСП, с зеркалом, высокий, широкие внутренние отделы."}',
    '3 eshikli kiyim shkafi',
    '{"uz": "3 eshikli kiyim shkafi", "ru": "3-дверный платяной шкаф"}',
    'dona', '210596ca-50a5-434a-b7d2-d046c4161de1', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 10. Kameralar (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Sony Alpha A7 IV kamera',
    '{"uz": "Sony Alpha A7 IV kamera", "ru": "Камера Sony Alpha A7 IV"}',
    'Sony Alpha A7 IV - full-frame, 33MP, 4K video, real-time AF, yorqin va aniq suratlar.',
    '{"uz": "Sony Alpha A7 IV - full-frame, 33MP, 4K video, real-time AF, yorqin va aniq suratlar.", "ru": "Sony Alpha A7 IV — полнокадровая, 33МП, 4K видео, реальный автофокус, яркие и четкие снимки."}',
    'Professional full-frame kamera',
    '{"uz": "Professional full-frame kamera", "ru": "Профессиональная полнокадровая камера"}',
    'dona', 'd5344e6f-1be5-4747-93bb-884bd37f8d2f', '3599076a-3729-448b-abe2-29f2c486b217', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Panasonic kuzatuv kamerasi',
    '{"uz": "Panasonic kuzatuv kamerasi", "ru": "Камера наблюдения Panasonic"}',
    'Panasonic kuzatuv kamerasi - 4MP, 1080p, tungi koʻrish, WiFi, harakat sensori, koʻcha uchun.',
    '{"uz": "Panasonic kuzatuv kamerasi - 4MP, 1080p, tungi koʻrish, WiFi, harakat sensori, koʻcha uchun.", "ru": "Камера наблюдения Panasonic — 4МП, 1080p, ночное видение, WiFi, датчик движения, для улицы."}',
    'Aqlli kuzatuv kamerasi',
    '{"uz": "Aqlli kuzatuv kamerasi", "ru": "Умная камера наблюдения"}',
    'dona', 'd5344e6f-1be5-4747-93bb-884bd37f8d2f', 'd41880af-0be0-46aa-9e3a-dc3934d3213a', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy web kamera',
    '{"uz": "Mahalliy web kamera", "ru": "Веб-камера местная"}',
    'Web kamera - 1080p Full HD, USB, mikrofonli, avtofokus, universal kronshteyn, Zoom/Video qoʻngʻiroqlar uchun.',
    '{"uz": "Web kamera - 1080p Full HD, USB, mikrofonli, avtofokus, universal kronshteyn, Zoom/Video qoʻngʻiroqlar uchun.", "ru": "Веб-камера — 1080p Full HD, USB, со встроенным микрофоном, автофокус, универсальный кронштейн, для Zoom/видеозвонков."}',
    'Full HD web kamera',
    '{"uz": "Full HD web kamera", "ru": "Full HD веб-камера"}',
    'dona', 'd5344e6f-1be5-4747-93bb-884bd37f8d2f', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 11. Kitoblar (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Oʻzbek tili darsligi',
    '{"uz": "Ozbek tili darsligi", "ru": "Учебник узбекского языка"}',
    'Oʻzbek tili darsligi - boshlangʻich daraja, grammatika, mashqlar, audio materiallar, yangi boshlovchilar uchun.',
    '{"uz": "Oʻzbek tili darsligi - boshlangʻich daraja, grammatika, mashqlar, audio materiallar, yangi boshlovchilar uchun.", "ru": "Учебник узбекского языка — начальный уровень, грамматика, упражнения, аудиоматериалы, для начинающих."}',
    'Oʻzbek tilini oʻrganish uchun qoʻllanma',
    '{"uz": "Oʻzbek tilini oʻrganish uchun qoʻllanma", "ru": "Пособие для изучения узбекского языка"}',
    'dona', 'e165a853-c5e5-44c1-8581-d06a1891740d', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bolalar uchun ertaklar toʻplami',
    '{"uz": "Bolalar uchun ertaklar toplami", "ru": "Сборник сказок для детей"}',
    'Bolalar ertaklari toʻplami - 50 ta eng sevimli ertaklar, rangli rasmlar, qattiq muqova, 3-7 yosh.',
    '{"uz": "Bolalar ertaklari toʻplami - 50 ta eng sevimli ertaklar, rangli rasmlar, qattiq muqova, 3-7 yosh.", "ru": "Сборник детских сказок — 50 любимых сказок, цветные иллюстрации, твердая обложка, 3-7 лет."}',
    'Eng sevimli bolalar ertaklari',
    '{"uz": "Eng sevimli bolalar ertaklari", "ru": "Любимые детские сказки"}',
    'dona', 'e165a853-c5e5-44c1-8581-d06a1891740d', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Pazandachilik kitobi',
    '{"uz": "Pazandachilik kitobi", "ru": "Книга по кулинарии"}',
    'Pazandachilik kitobi - 200 ta milliy va xalqaro retseptlar, bosqichma-bosqich koʻrsatmalar, rangli fotolar.',
    '{"uz": "Pazandachilik kitobi - 200 ta milliy va xalqaro retseptlar, bosqichma-bosqich koʻrsatmalar, rangli fotolar.", "ru": "Кулинарная книга — 200 национальных и международных рецептов, пошаговые инструкции, цветные фото."}',
    'Mazali retseptlar toʻplami',
    '{"uz": "Mazali retseptlar toplami", "ru": "Сборник вкусных рецептов"}',
    'dona', 'e165a853-c5e5-44c1-8581-d06a1891740d', '4160f372-556b-4a42-a4ca-8a9699004a50', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 12. Kanselyariya (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy daftar (A5)',
    '{"uz": "Mahalliy daftar A5", "ru": "Местная тетрадь А5"}',
    'Daftar A5 - 96 varaq, qattiq muqova, katakli/chiziqli, sifatli qogʻoz, maktab va ofis uchun.',
    '{"uz": "Daftar A5 - 96 varaq, qattiq muqova, katakli/chiziqli, sifatli qogʻoz, maktab va ofis uchun.", "ru": "Тетрадь А5 — 96 листов, твердая обложка, в клетку/линейку, качественная бумага, для школы и офиса."}',
    'Sifatli maktab daftari',
    '{"uz": "Sifatli maktab daftari", "ru": "Качественная школьная тетрадь"}',
    'dona', '8c38c710-bc78-41fa-b2a2-1f690a43a2e7', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy qalam toʻplami',
    '{"uz": "Mahalliy qalam toplami", "ru": "Набор ручек местный"}',
    'Qalam toʻplami - 12 xil rang, silliq yozish, uchburchak tutqich, uzoq muddat foydalanish.',
    '{"uz": "Qalam toʻplami - 12 xil rang, silliq yozish, uchburchak tutqich, uzoq muddat foydalanish.", "ru": "Набор ручек — 12 цветов, плавное письмо, треугольный захват, долговечные."}',
    '12 rangli qalam toʻplami',
    '{"uz": "12 rangli qalam toplami", "ru": "Набор ручек 12 цветов"}',
    'dona', '8c38c710-bc78-41fa-b2a2-1f690a43a2e7', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Samsung Life marker toʻplami',
    '{"uz": "Samsung Life marker toplami", "ru": "Набор маркеров Samsung Life"}',
    'Marker toʻplami - 8 xil rang, suv bazali, koʻpchilik sirtlarda yozadi, ofis va maktab uchun.',
    '{"uz": "Marker toʻplami - 8 xil rang, suv bazali, koʻpchilik sirtlarda yozadi, ofis va maktab uchun.", "ru": "Набор маркеров — 8 цветов, на водной основе, пишет на большинстве поверхностей, для офиса и школы."}',
    '8 rangli marker toʻplami',
    '{"uz": "8 rangli marker toplami", "ru": "Набор маркеров 8 цветов"}',
    'dona', '8c38c710-bc78-41fa-b2a2-1f690a43a2e7', 'e17e92d2-798b-4c10-9be0-b64af577c3ed', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 13. Bogʻ va tomorqa (2 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Bosch bogʻ qaychisi',
    '{"uz": "Bosch bog qaychisi", "ru": "Садовые ножницы Bosch"}',
    'Bosch bogʻ qaychisi - akkumulyatorli, 2 soat ishlash, 15mm kesish, yengil va ergonomik.',
    '{"uz": "Bosch bogʻ qaychisi - akkumulyatorli, 2 soat ishlash, 15mm kesish, yengil va ergonomik.", "ru": "Садовые ножницы Bosch — аккумуляторные, 2 часа работы, срез 15мм, легкие и эргономичные."}',
    'Akkumulyatorli bogʻ qaychisi',
    '{"uz": "Akkumulyatorli bog qaychisi", "ru": "Аккумуляторные садовые ножницы"}',
    'dona', '570b718c-990c-44e0-957b-78959bf59761', 'ca6ecd9d-225a-4a32-b96c-755c286d3386', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel bogʻ shlangi',
    '{"uz": "Artel bog shlangi", "ru": "Садовый шланг Artel"}',
    'Artel bogʻ shlangi - 30m, 1/2 dyum, mustahkam PVC, UV himoya, suv oʻtkazmaydigan ulagichlar.',
    '{"uz": "Artel bogʻ shlangi - 30m, 1/2 dyum, mustahkam PVC, UV himoya, suv oʻtkazmaydigan ulagichlar.", "ru": "Садовый шланг Artel — 30м, 1/2 дюйма, прочный ПВХ, UV защита, водонепроницаемые соединители."}',
    '30 metrli bogʻ shlangi',
    '{"uz": "30 metrli bog shlangi", "ru": "Садовый шланг 30 метров"}',
    'dona', '570b718c-990c-44e0-957b-78959bf59761', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 14. Oʻyinchoqlar (3 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'LG bolalar konstruktori',
    '{"uz": "LG bolalar konstruktori", "ru": "Детский конструктор LG"}',
    'Bolalar konstruktori - 100 detall, plastik, ekologik toza, 3+ yosh, motorika va fikrlashni rivojlantiradi.',
    '{"uz": "Bolalar konstruktori - 100 detall, plastik, ekologik toza, 3+ yosh, motorika va fikrlashni rivojlantiradi.", "ru": "Детский конструктор — 100 деталей, пластик, экологичный, 3+ лет, развивает моторику и мышление."}',
    '100 detalli bolalar konstruktori',
    '{"uz": "100 detalli bolalar konstruktori", "ru": "Детский конструктор 100 деталей"}',
    'dona', '048d953b-3372-403d-8165-1feb7a4244ec', 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Mahalliy yumshoq oʻyinchoq',
    '{"uz": "Mahalliy yumshoq oyinchoq", "ru": "Мягкая игрушка местная"}',
    'Yumshoq oʻyinchoq ayiqcha - 30 sm, yuqori sifatli peluş, gipoallergen toʻldiruvchi, 0+ yosh.',
    '{"uz": "Yumshoq oʻyinchoq ayiqcha - 30 sm, yuqori sifatli peluş, gipoallergen toʻldiruvchi, 0+ yosh.", "ru": "Мягкая игрушка мишка — 30 см, высококачественный плюш, гипоаллергенный наполнитель, 0+ лет."}',
    'Yumshoq ayiqcha 30 sm',
    '{"uz": "Yumshoq ayiqcha 30 sm", "ru": "Мягкий мишка 30 см"}',
    'dona', '048d953b-3372-403d-8165-1feb7a4244ec', '6ab89b9c-9219-493f-aa15-f1011529fa05', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Artel bolalar velosipedi',
    '{"uz": "Artel bolalar velosipedi", "ru": "Детский велосипед Artel"}',
    'Artel bolalar velosipedi - 12 dyum gʻildirak, yordamchi gʻildiraklar, qoʻngʻiroq, yorqin rang, 3-6 yosh.',
    '{"uz": "Artel bolalar velosipedi - 12 dyum gʻildirak, yordamchi gʻildiraklar, qoʻngʻiroq, yorqin rang, 3-6 yosh.", "ru": "Детский велосипед Artel — 12-дюймовые колеса, вспомогательные колесики, звонок, яркий цвет, 3-6 лет."}',
    'Bolalar uchun birinchi velosiped',
    '{"uz": "Bolalar uchun birinchi velosiped", "ru": "Первый велосипед для ребенка"}',
    'dona', '048d953b-3372-403d-8165-1feb7a4244ec', '9a771ea7-cf15-407c-aebf-b3d483655005', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- 15. Uy hayvonlari (2 ta)
-- ===================================================================
INSERT INTO product (id, barcode, name, name_translations, description, description_translations, short_description, short_description_translations, unit, category_id, brand_id, lead_time_days, min_stock, weight, length, width, height, fragile, active, created_at, updated_at)
VALUES
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Nestle mushuk ozuqasi',
    '{"uz": "Nestle mushuk ozuqasi", "ru": "Корм для кошек Nestle"}',
    'Nestle mushuk ozuqasi - 1.5kg, quruq, toʻliq ratsion, vitaminlar, taurin bilan boyitilgan.',
    '{"uz": "Nestle mushuk ozuqasi - 1.5kg, quruq, toʻliq ratsion, vitaminlar, taurin bilan boyitilgan.", "ru": "Корм для кошек Nestle — 1.5кг, сухой, полнорационный, обогащен витаминами и таурином."}',
    'Mushuklar uchun toʻliq ratsion',
    '{"uz": "Mushuklar uchun toʻliq ratsion", "ru": "Полнорационный корм для кошек"}',
    'dona', '3b0595fc-392b-4b7c-b94d-48ab1147bae5', '3fa752a2-3484-4797-8c9a-957481b003c9', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  ),
  (
    gen_random_uuid(), gen_random_uuid()::text,
    'Nestle it ozuqasi',
    '{"uz": "Nestle it ozuqasi", "ru": "Корм для собак Nestle"}',
    'Nestle it ozuqasi - 3kg, quruq, katta zotlar uchun, protein va omega-3 ga boy, sogʻlom tish va jun.',
    '{"uz": "Nestle it ozuqasi - 3kg, quruq, katta zotlar uchun, protein va omega-3 ga boy, sogʻlom tish va jun.", "ru": "Корм для собак Nestle — 3кг, сухой, для крупных пород, богат белком и омега-3, здоровые зубы и шерсть."}',
    'Itlar uchun proteinli ozuqa',
    '{"uz": "Itlar uchun proteinli ozuqa", "ru": "Белковый корм для собак"}',
    'dona', '3b0595fc-392b-4b7c-b94d-48ab1147bae5', '3fa752a2-3484-4797-8c9a-957481b003c9', 0, 0, 0.0, 0.0, 0.0, 0.0, false, true, now(), now()
  );

-- ===================================================================
-- Now insert product prices: one sale price per product (effective today, no end_date)
-- ===================================================================
-- We need to capture the IDs we just inserted. Using a simpler approach:
-- insert prices referencing products by (name, brand_id) pairs.

DO $$
DECLARE
    p_id uuid;
BEGIN
    -- Elektronika
    SELECT id INTO p_id FROM product WHERE name = 'iPhone 15 Pro Max' AND brand_id = 'e85b6ef2-6a3e-423b-a654-360533bebf08';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 12000000, 14999000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung Galaxy S24 Ultra' AND brand_id = '4a7e55be-ec37-4d30-93ee-63ae66b0ae06';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 11000000, 13499000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Xiaomi Redmi Note 13 Pro' AND brand_id = '063dfdf2-32c3-4723-b732-bd253309685e';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 3500000, 4299000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Sony WH-1000XM5 naushniklari' AND brand_id = '3599076a-3729-448b-abe2-29f2c486b217';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 3000000, 3799000, now());

    SELECT id INTO p_id FROM product WHERE name = 'LG 43" 4K Ultra HD televizor' AND brand_id = 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 4500000, 5799000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Apple MacBook Air M3' AND brand_id = 'e85b6ef2-6a3e-423b-a654-360533bebf08';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 13000000, 15999000, now());

    -- Uy jihozlari
    SELECT id INTO p_id FROM product WHERE name = 'Bosch kir yuvish mashinasi' AND brand_id = 'ca6ecd9d-225a-4a32-b96c-755c286d3386';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 6800000, 8499000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Artel muzlatgichi' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 3600000, 4599000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung mikrodolqinli pech' AND brand_id = '4a7e55be-ec37-4d30-93ee-63ae66b0ae06';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 1800000, 2399000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Panasonic changyutkichi' AND brand_id = 'd41880af-0be0-46aa-9e3a-dc3934d3213a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 2500000, 3199000, now());

    SELECT id INTO p_id FROM product WHERE name = 'LG konditsioneri' AND brand_id = 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 4300000, 5499000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Artel elektr choynak' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 200000, 299000, now());

    -- Kiyim-kechak
    SELECT id INTO p_id FROM product WHERE name = 'Adidas erkaklar sport krossovkalari' AND brand_id = 'abc4e0ab-d557-40b5-b930-ab0147ef550a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 900000, 1299000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Erkaklar futbolkasi (paxta)' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 100000, 149000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Ayollar paltosi' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 900000, 1299000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Adidas erkaklar shimi' AND brand_id = 'abc4e0ab-d557-40b5-b930-ab0147ef550a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 400000, 599000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Bolalar kurtkasi' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 320000, 449000, now());

    -- Oziq-ovqat
    SELECT id INTO p_id FROM product WHERE name = 'Nestle sutli shokolad' AND brand_id = '3fa752a2-3484-4797-8c9a-957481b003c9';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 18000, 25000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Nestle kofe 3v1' AND brand_id = '3fa752a2-3484-4797-8c9a-957481b003c9';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 24000, 32000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy asal (1L)' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 85000, 120000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy yogʻ (500g)' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 45000, 65000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Nestle chaqaloq ovqati' AND brand_id = '3fa752a2-3484-4797-8c9a-957481b003c9';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 32000, 45000, now());

    -- Ichimliklar
    SELECT id INTO p_id FROM product WHERE name = 'Coca-Cola 1.5L' AND brand_id = '2db4e1d7-e128-4df9-b216-e2bb58f97356';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 12000, 18000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Pepsi 1L' AND brand_id = '9b2e86a2-b165-437f-97f3-a67384b1e622';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 10000, 15000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Coca-Cola Zero 0.5L' AND brand_id = '2db4e1d7-e128-4df9-b216-e2bb58f97356';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 7000, 10000, now());

    SELECT id INTO p_id FROM product WHERE name = 'PepsiCo Lays chipslari' AND brand_id = '9b2e86a2-b165-437f-97f3-a67384b1e622';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 16000, 22000, now());

    -- Go'zallik
    SELECT id INTO p_id FROM product WHERE name = 'Samsung Life tish pastasi' AND brand_id = 'e17e92d2-798b-4c10-9be0-b64af577c3ed';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 25000, 35000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung Life sovuni' AND brand_id = 'e17e92d2-798b-4c10-9be0-b64af577c3ed';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 12000, 18000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung Life shampun' AND brand_id = 'e17e92d2-798b-4c10-9be0-b64af577c3ed';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 32000, 45000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy efir moyi' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 38000, 55000, now());

    -- Sport
    SELECT id INTO p_id FROM product WHERE name = 'Adidas futbol toʻpi' AND brand_id = 'abc4e0ab-d557-40b5-b930-ab0147ef550a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 250000, 349000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Adidas gimnastika toʻshagi' AND brand_id = 'abc4e0ab-d557-40b5-b930-ab0147ef550a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 140000, 199000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy gantel toʻplami' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 350000, 499000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung Life termosi' AND brand_id = 'e17e92d2-798b-4c10-9be0-b64af577c3ed';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 90000, 129000, now());

    -- Avtotovarlar
    SELECT id INTO p_id FROM product WHERE name = 'Bosch avtomobil akkumulyatori' AND brand_id = 'ca6ecd9d-225a-4a32-b96c-755c286d3386';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 650000, 899000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Bosch avtomobil moyi' AND brand_id = 'ca6ecd9d-225a-4a32-b96c-755c286d3386';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 250000, 349000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Artel avtomobil changyutkichi' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 170000, 249000, now());

    -- Mebel
    SELECT id INTO p_id FROM product WHERE name = 'Artel oshxona stoli' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 1100000, 1499000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy yumshoq divan' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 3000000, 3999000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy shkaf' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 3800000, 4999000, now());

    -- Kameralar
    SELECT id INTO p_id FROM product WHERE name = 'Sony Alpha A7 IV kamera' AND brand_id = '3599076a-3729-448b-abe2-29f2c486b217';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 20000000, 24999000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Panasonic kuzatuv kamerasi' AND brand_id = 'd41880af-0be0-46aa-9e3a-dc3934d3213a';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 500000, 699000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy web kamera' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 140000, 199000, now());

    -- Kitoblar
    SELECT id INTO p_id FROM product WHERE name = 'Oʻzbek tili darsligi' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 60000, 85000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Bolalar uchun ertaklar toʻplami' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 45000, 65000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Pazandachilik kitobi' AND brand_id = '4160f372-556b-4a42-a4ca-8a9699004a50';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 70000, 95000, now());

    -- Kanselyariya
    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy daftar (A5)' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 8000, 12000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy qalam toʻplami' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 17000, 25000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Samsung Life marker toʻplami' AND brand_id = 'e17e92d2-798b-4c10-9be0-b64af577c3ed';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 25000, 35000, now());

    -- Bogʻ va tomorqa
    SELECT id INTO p_id FROM product WHERE name = 'Bosch bogʻ qaychisi' AND brand_id = 'ca6ecd9d-225a-4a32-b96c-755c286d3386';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 400000, 549000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Artel bogʻ shlangi' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 140000, 199000, now());

    -- Oʻyinchoqlar
    SELECT id INTO p_id FROM product WHERE name = 'LG bolalar konstruktori' AND brand_id = 'ac07ab7a-e26f-47ac-a31d-b3ce5e935d4b';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 100000, 149000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Mahalliy yumshoq oʻyinchoq' AND brand_id = '6ab89b9c-9219-493f-aa15-f1011529fa05';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 60000, 89000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Artel bolalar velosipedi' AND brand_id = '9a771ea7-cf15-407c-aebf-b3d483655005';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 420000, 599000, now());

    -- Uy hayvonlari
    SELECT id INTO p_id FROM product WHERE name = 'Nestle mushuk ozuqasi' AND brand_id = '3fa752a2-3484-4797-8c9a-957481b003c9';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 55000, 75000, now());

    SELECT id INTO p_id FROM product WHERE name = 'Nestle it ozuqasi' AND brand_id = '3fa752a2-3484-4797-8c9a-957481b003c9';
    INSERT INTO product_price (id, product_id, cost_price, sale_price, effective_date) VALUES (gen_random_uuid(), p_id, 85000, 120000, now());

    RAISE NOTICE 'All 50 product prices inserted successfully';
END $$;


