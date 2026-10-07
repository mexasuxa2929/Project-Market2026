# Project_Market — Yuqori aniqlikdagi audit hisoboti

**Sana:** 2026-10-05 · **Metod:** statik tahlil (4 ta mustaqil agent: backend, delivery-pricing, desktop, mobile) + build tekshiruvi
**Hajm:** 15 modul (14 xizmat + realtime-lib), 399 backend endpoint, desktop 130 .kt / 38 289 qator, mobile 50 endpoint / 23 ekran
**Build holati:** desktop `compileKotlinJvm` = 0 xato · mobile `compileDebugKotlin` = SUCCESS · backend 536 ta importning barchasi topdi (compile bloklanmaydi)

> **Muhim:** repo'da **133 ta commitilmagan o'zgarish** va **1 ta commit** (`40e8ff6 september21`) bor — butun delivery-pricing moduli faqat working tree'da. `git checkout` = haftalik ish yo'qoladi.

---

## I. Xulosa — eng jiddiy 10 ta topilma

| # | Muammo | Joy |
|---|---|---|
| 1 | **geo `/internal/**` autentifikatsiyasiz** — har kim zonani/o'rnini o'chira oladi | `geo-service SecurityConfig.java:30`, `InternalGeoController.java:46`, gateway yml:299 |
| 2 | **payment `/api/**` permitAll** — pullik buyurtma yaratish mumkin | `payment SecurityConfig.java:23-24`, `OrderController.java:20,36` |
| 3 | **Narx qoidasi "0" bilan yozilsa → barcha buyurtmaga bepul delivery** (`pick()` 0 va null ni ajrata olmaydi) | `DeliveryPricingService.java:86-90`, `PricingRule.java:39-53` |
| 4 | **Checkout'da ko'rsatilgan narx ≠ order'ga yozilgan narx** | mobile `CheckoutScreen.kt:142,220,630` vs `OrderService.java:486-494` |
| 5 | **Desktop'da couriers/deliveries ekranlari o'chirilib qayta yaratilmagan** — 40 ta backend endpoint UI'siz | `git status` (D `ui/couriers/*`, `ui/delivery/*`) |
| 6 | **Mobile `BASE_URL = http://192.168.1.6:8080/` + cleartext + keystore paroli git'da** | `RetrofitClient.kt:18`, `AndroidManifest.xml:19`, `build.gradle.kts:14-17` |
| 7 | **Report-service Feign'siz (auth'siz) chaqiradi → hisobotlar doim 0** | `report-service ReportService.java:134-137`, Feign interceptor yo'q |
| 8 | **Refund = faqat DB yozuvi, Payme'dan pul qaytarilmaydi** (payment'da refund umuman yo'q) | `OrderService.java:557-578`, `grep refund payment-service` = 0 |
| 9 | **Delivery FAILED/RETURNED → order'ga xabar YO'Q** — order doimik SHIPPING qoladi | `DeliveryService.java:213,615` vs `OrderInternalClient.java:19-23` |
| 10 | **15 modulning 11 tasida test yo'q, CI yo'q** (9 test class / 26 test method butun backend uchun) | `find */src/test`, `.github/` = mavjud emas |

---

## II. CRITICAL

### A. Backend xavfsizligi
1. **geo-service ichki endpointlari ochiq.** `SecurityConfig.java:30` → `/internal/**` permitAll; `InternalGeoController.java:46-51` da `X-Internal-Secret` tekshiruvi **yo'q** (boshqa xizmatlarda bor). Gateway `application.yml:299` esa `/internal/geo-zones/**` ni internetga chiqaradi. **Natija:** anonim `DELETE /internal/geo-zones/by-warehouse/{id}` butun yetkazish zonasi va ombor lokalizatsiyasini o'chiradi.
2. **payment-service hamma narsa ochiq.** `SecurityConfig.java:23-24` → `/api/**` permitAll; `order/OrderController.java:20,36` autentifikatsiyasiz to'lov buyurtmasi yaratadi (istalgan `userId`, `amount`). (`/payment/payme/**` ochiq to'g'ri — Payme Basic auth ishlatadi.)
3. **product-service fail-open.** `security/SecurityConfig.java:61` → `.anyRequest().permitAll()` (boshqa 10 xizmat `authenticated` bilan yopadi). Yangi endpoint default-ochiq bo'ladi.
4. **SSE hamma narsani hamma ga yuboradi.** `gateway RealtimeSseController.java:51-62` bitta Redis kanalidan butun xabarni forward qiladi; payload'da boshqa user'larning `orderId/userId/notificationType` (`OrderRealtimePublisher.java:34-46`). `MAX_SSE_CONNECTIONS=100` hardcoded, per-user limit yo'q.

### B. Delivery pricing mantiqi
5. **`pick()` 0 va null ni ajrata olmaydi.** `DeliveryPricingService.java:86-90` — qoida `baseFee/perKm/perKg/minFee = 0` (`PricingRule.java:39-53`, DB `DEFAULT 0`) bilan yozilsa, zona va default stavkalar **butunlay e'tiborsiz** → narx 0 → **hamma buyurtmaga bepul yetkazish**. Desktop ZONALAR tab'idagi "Zona narxi qoidadan oldin olinadi" matni kod bilan ziddiyatda (`DeliveryPricingScreen.kt:730`).
6. **"Bepul yetkazish" qoidasi narxni OSHIRISHI mumkin.** `DeliveryPricingService.java:142-146` — `discount = beforeDiscount - deliveryFee` clamp'siz; `deliveryFee > beforeDiscount` bo'lsa mijoz ko'proq to'laydi, `discount` manfiy → `:168` da 0 ga tushirilib breakdown yashiradi. `rewardValue < 0` (AMOUNT) ham xuddi shunday (`:159`).
7. **Yozuvlarda validatsiya yo'q.** `PricingAdminController.java:111-117,147-153,218-223` — `@Valid` yo'q: manfiy `minOrder`, `threshold=0`, `rewardValue>100`, `startAt>endAt`, `fee<0` → to'g'ridan-to'g'ri noto'g'ri hisob (yoki 500).
8. **Bepul-qoida/incentive uchun sana rejalashtirish GUI'da yo'q** (`CartIncentiveRule.startAt/endAt` backendda bor, `incentiveFields` da yo'q — `DeliveryPricingScreen.kt:1117-1135`).

### C. Pul/summa ko'rinishi
9. **Mobile checkout noto'g'ri summa ko'rsatadi** — 3 mustaqil sabab:
   - quote faqat `orderValue` yuboradi (`CheckoutScreen.kt:142`, `DeliveryPricingApiService.kt:14-16` — `region/district/zoneId/distance/type` null), server esa manzildan zone aniqlab boshqa narx hisoblaydi (`OrderService.java:486-494`);
   - chegirma kodi yig'iladi (`:92,480`) lekin umumiy summada **ayirilmaydi** (`:220,630`), serverda `total = subtotal + fee − discount` (`OrderService.java:215,514`);
   - quote yiqilsa `fee=0` → "Bepul" chiqadi, tugma `feeReady` ga qaramay ochiq (`:149-150,204,602`).
10. **Order yaratilganda fee=0, keyin afterCommit'da tuzatiladi** — manzil/geocode/Feign xatosi bo'lsa (`OrderService.java:430-498`) `log.warn` bilan yutiladi va **order doim bepul qoladi** (retry yo'q). Realtime/xabar `:225` noto'g'ri summa bilan ketadi.

### D. Git/hujjat gigienasi
11. **133 commitilmagan o'zgarish** (butun `delivery-service/.../pricing/`, V3–V7 migratsiyalar, gateway route'lari, desktop Delivery sahifasi, mobile quote) + **1 commit** tarix + **CI yo'q** → `git clone` boshqacha tizim beradi.
12. **Mobil imzolash siri git'da:** `build.gradle.kts:14-17` `storePassword/keyPassword = "<maxfiy qiymat>"`, `release-key.jks` esa repo'da yo'q (release build ishlamaydi).

---

## III. HIGH

### Backend
- **H1. report-service Feign'lari auth'siz** → `/api/reports/*` doim bo'sh (`ReportService.java:134-137` `return List.of()`), interceptor yo'q.
- **H2. Delivery → order faqat DELIVERED'da sinxron** (`DeliveryService.java:88-96`, `:190,208`); `fail()` `:213`, `returnDelivery()` `:615` order'ga xabar bermaydi → order doim SHIPPING, stok/diskont qaytarilmaydi, analitika buziladi.
- **H3. Refund haqiqatda yo'q** — `OrderService.java:557-578` faqat DB, payment-service'da `refund/cancelTransaction` 0 ta topilma.
- **H4. Payme → order xabari yutiladi** (`payment OrderServiceClient.java:44-55` `log.warn`) → PAID/UNPAID divergensiya, reconciliation job yo'q.
- **H5. Gateway 6 ta `/internal/**` route'ini internetga chiqaradi** (yml:215,227,239,263,275,299); geo'da secret tekshiruvi yo'q (yuqori #1). Gateway `SecurityConfig.java:28` `anyExchange().permitAll()`.
- **H6. Reference data mavjud bo'lmagan xizmatga tayanadi** — `docker-compose.yml:134` `catalog-service:8086` (xizmat umuman yo'q), kod defaulti `localhost:8084` = warehouse (katalog jadvallari o'chirilgan `V4__drop_local_catalog_tables.sql`) → kategoriya/brand nomlari null, CSV import ishlamaydi.
- **H7. Gateway mahsulot route'lari rol NOMI bo'yicha** (yml:163-186) → `ROLE_ADMIN` bo'lmagan rollar uchun `/api/products/{id}/prices` 404; yaratish endpointi esa shop-service'ga tushib 404 beradi.
- **H8. Notification o'z bildirishnomalariga 403** — `notification SecurityConfig.java:33` `NOTIFICATION_VIEW` faqat SUPER_ADMIN'da (`V6__...sql:59`).
- **H9. product-service `ddl-auto: update` + Flyway birga** (`application.yml:30,41-46`) → schema drift; warehouse'da **V11 migratsiyasi yo'q** (`V10__` → `V12__`); `validate-on-migrate=false` (shop:33, warehouse:27).
- **H10. Order status o'zgarishi — state machine yo'q** (`OrderService.java:379-401`): CANCELLED qilinsa stok/diskont/delivery qaytarilmaydi.
- **H11. Istalgan joyda commit qilingan sirlar:** JWT secret 13 faylda (`mexa_jwt_2024_secret_key_32chars_long!!`), `APP_INTERNAL_SECRET` 12 faylda, DB parol `mexa1234`, swagger `swagger/change-me-now`, OTP `change-me`, `BlockedUserFilter.java:92` literal secret, `RoleRoutePredicateFactory.java:25` boshqa default.
- **H12. Search xizmati o'chirilgan** — `search application.yml:20` `elasticsearch.enabled: false` → `@ConditionalOnProperty` barcha bean'larni yaratmaydi, 2 ta gateway route 404.
- **H13. Login blokirovkasi in-memory** (`LoginAttemptService.java:15`) — restart/replicada ishlamaydi.

### Delivery-pricing
- **D-H1. `distanceKm` hech qayerda hisoblanmaydi** — `haversineKm` (`DeliveryService.java:559`) hech qayerda chaqirilmaydi; `InternalDeliveryCreateRequest` da `distanceKm/deliveryType` yo'q → **km formulining yarmi production'da o'lik**, `avgDeliveryDistance` doim 0.
- **D-H2. Vaqt mintaqasi UTC** (`docker-compose.yml` da `TZ` yo'q) → 18:00–21:00 peak 13:00–16:00 da ishlaydi (5 soat siljish). `DeliverySurcharge` da sana (`startAt/endAt`) ustuni yo'q → HOLIDAY butun yil davomida.
- **D-H3. UI va engine surcharge turlari mos emas** — UI `DISTANCE/WEIGHT/BETWEEN` taklif qiladi (`DeliveryPricingScreen.kt:1141-1146`), engine esa `default -> orderValue` bilan solishtiradi va `BETWEEN` ishlamaydi (`DeliveryPricingService.java:290-333`) → "masofa > 10 km" qoidasi aslida "savat ≥ 10 so'm" bo'ladi.
- **D-H4. Analytics faqat bitta yo'lda yoziladi** — `recordEvent=true` faqat `DeliveryService.create` (`:108-113`); quote/incentive/simulator/legacy `false`. `exposure` = " chegara yetgan orderlar", haqiqiy exposure emas → conversion statistikasi buziladi.
- **D-H5. `PricingEvent` da qoida id/raqami yo'q** → "eng yaxshi qoidalar", "surcharge qo'llanilish soni" hisoblab bo'lmaydi (sxema o'zgarishi kerak).
- **D-H6. Performance:** har quote'da 4 ta to'liq tablica skani (`:220,226,271,281`), `@Transactional` write; analytics'da `findRecent(since).size()` (`PricingAnalyticsService.java:66`) butun jadvalni xotiraga oladi; N+1 threshold so'rovlari. Pool = 5.
- **D-H7. Desktop'da VERSIONS va AUDIT tablari umuman yo'q** (`/versions`, `/audit` endpointlari ishlatilmaydi); **zona yaratish/o'chirish yo'q** (faqat tahrirlash); delete sabab qabul qilinmaydi (`ConfirmDeleteDialog(requireReason=false)`, backendda ham `delete*(id)` reason olmaydi) → buzuvchi amallar auditable emas.
- **D-H8. `buildJson` bo'sh qiymatlarni tashlaydi** (`DeliveryPricingDialogs.kt:100,104,112`) → `startAt/endAt`, `maxFee`, `zoneId`, `deliveryType`, `conditionValue` hech qachon **tozalanmaydi** (eski qiymet saqlanib qoladi, xatosiz).
- **D-H9. O'chirilgan delivery type ham narx oladi** — `findByNameIgnoreCase` da `active` filtri yo'q (`DeliveryTypeRepository.java:10`), `type.baseFee` engine'da o'lik lekin UI'da tahrirlanadi.
- **D-H10. `zone.freeAbove` engine'da umuman o'qilmaydi** (legacy V3 semantikasi yo'qolgan), lekin `DeliveryPublicController.java:78` hali dokumentatsiyada.

### Desktop
- **T-H1. Global qidiruv hech narsaga ulanmagan** — `SuperAdminDashboardLayout.kt:88` `searchQuery` → `SuperAdminTopBar.kt:58` ko'rsatiladi, hech qayerga uzatilmaydi.
- **T-H2. SSE'da HTTP xatosiga backoff yo'q** — `ApiClient.kt:345-352` xato qaytarmaydi, `GatewayRealtimeHub.kt:61-72` `delay`siz aylanadi (faqat exception uchun `:78` delay) → gateway 503 bo'lsa CPU 100%.
- **T-H3. Refresh xatosi = session tugadi** — `TokenRefreshManager.kt:44-47` tarmoq xatosini ham `AuthSession.clear()` + forced logout qiladi (15 soniyalik poll'da bitta packet yo'qolsa — chiqish).
- **T-H4. Soxta paginatsiya (5 ta ro'yxat)** — `AuditLogScreen.kt:71` (`size=200`), `NotificationsScreen.kt:103` (200), `OrdersScreen.kt:165` (100), `WarehousesScreen.kt:120`, `TemplatesScreen.kt:71` → bitta sahifa olinib clientda bo'linadi; **audit 200 tadan keyin ko'rinmaydi**.
- **T-H5. Lokalizatsiya deyarli yo'q** — 58 fayldan 50 tasida `AppStrings` ishlatilmaydi; 612 ta `Text("` dan 591 tasi (96%) hardcoded; RU rejimida ham o'zbekcha; 95 ta string ishlatilmaydi; EN til umuman yo'q.
- **T-H6. Foydalanuvchi yaratish mumkin emas** — `AddUserDialog.kt:58` hech qayerda chaqirilmaydi, `UsersScreenModel.createUser:144` o'lik, backend `POST /api/admin/users` callsiz.
- **T-H7. Realtime filtr ro'yxatlarga to'liq mos emas** — `GatewayRealtime.kt:9-25` da `/roles` yo'q → `RolesScreen:59` subscription o'lik; pricing/review/template/notif mutationlari umuman chiqmaydi.
- **T-H8. 5 ta butun domen UI'siz:** analytics (7 endpoint), promotions/discounts (8), groups (7), search, product ops (export/import/barcode/batch/stock-summary) + `pricing/versions|audit`, `delivery-zones` yaratish/o'chirish.

### Mobile
- **M-H1. SSE `Authorization` headersiz** → gateway 401 (`RealtimeEventService.kt:34`, gateway `SecurityConfig.java:27`), 10 soniyada takrorlanadi, socket yopilmaydi (`:40-41`) → realtime hech qachon ishlamaydi.
- **M-H2. Refresh xatosi = tokenlarni o'chirish** — `TokenAuthenticator.kt:67-78` offline/timeout/5xx ni ham `TokenManager.clear()` qiladi; `:37` 403 da ham refresh; `:88-91` har safar yangi OkHttpClient; `:105-118` response yopilmaydi.
- **M-H3. Savat miqdori SET, emas INCREMENT** — `AppNavigation.kt:298,405` `updateItem(id, 1)` → mavjud 3 ta mahsulot **1 ga tushadi**; `CartViewModel.kt:75-79` state Success bo'lmasa jim o'tkazadi; `:107-113` `?: 1`.
- **M-H4. Logout faqat lokal** — `AuthRepository.kt:239`; `AuthApiService.logout` (`:48-49`) hech chaqirilmaydi; `/api/auth/sessions` endpointlari ishlatilmaydi → o'g'irlingan qurilmada refresh token qoladi.
- **M-H5. "Savatga qo'shildi" Toast serverdan OLDIN chiqadi** (`HomeScreen.kt:117-122`).
- **M-H6. Narx filtri backend'da ishlamaydi** — `shop ProductController.java:50-67` `minPrice/maxPrice/currency` qabul qiladi lekin `getProductsPage` ga uzatmaydi.

---

## IV. MEDIUM (qisqacha)

**Backend:** gateway'da `mavjud bo'lmagan route'lar` (`/api/suppliers/**` — o'lik, `V18__drop_supplier.sql`); `/api/stock/**` route'siz (StockBatch ishlamaydi); payment `/api/orders` order-service'ga ketadi (404); CORS `*` (gateway `CorsConfig.java:22-24`, `GATEWAY_CORS_ALLOWED_ORIGIN_*` o'lik); SSE'da xom `path` broadcast (`RealtimeMutationPublishFilter.java:71-77`); `.cursor-tmp-flyway*` 4 ta framework fayli git'da; `docs/PRODUCTION-REVIEW.md` eskirgan; JWT parsing 11+ xizmatda copy-paste; swagger default parol.

**Delivery-pricing:** V7'da indeks yetishmaydi (`pricing_events` faqat `created_at/order_id` — `cart_total/zone_id/threshold` yo'q; `free_delivery_rules/cart_incentive_rules/delivery_surcharges` **0 ta indeks**); CHECK constraintlar yo'q; `bumpVersion` faqat CREATE'da (UPDATE/DELETE/SETTINGS'da yo'q), concurrent'da UNIQUE collision; audit `catch(Exception ignored)` (`PricingAdminController.java:354`) va `findAll()` + 200 ga kesish; fee override (`PUT /deliveries/{id}/fee`) auditsiz; `settings()` GET'da yozuv qiladi (`:393-397`); `zoneId` noto'g'ri bo'lsa jim `null` zona; `buildIncentive` bepul yetkazishdan keyin ham "qo'shing" deydi.

**Desktop:** 8 parallel HTTP → bitta xato butun sahifani buzadi (`DeliveryPricingScreen.kt:116-142`); per-tab loading/refresh/`days` tanlov yo'q; pagination yo'q; `TypeTab` 4+ turga siqiladi; preview/settings xatolari yutiladi (`:338,411`); product qidiruvida `URLEncoder` yo'q (`ProductsScreen.kt:187-189`); 401 da refresh urinishi yo'q (`ApiClient.kt` 8 ta joy); realtime 4 ekranda; o'lik tugmalar (17 ta `onClick={}`, `AddUserDialog`, `DeleteConfirmDialog`, `MarketBridge.kt`); `success` flag hech qayerda tekshirilmaydi.

**Mobile:** katalogda error/bo'sh holat yo'q (`CatalogScreen`, `Profile`, `Settings`, `Help`, `PaymentMethods`, `MapPicker`, 3 ta auth ekrani); katalogda filter/sort umuman yo'q; `CatalogViewModel` o'lik; manzil saqlash xatosida forma yopiladi (`AddressViewModel.kt:98-112`); manzilda telefon/viloyat/tuman maydonlari yo'q (kalitlar bor, input yo'q); notification 50 tadan keyin ko'rinmaydi; push/FCM yo'q (`google-services.json` git'da lekin plugin yo'q); to'lov demo kartalar (`PaymentMethodsScreen.kt:252`); 89 ta ishlatilmagan tarjima kaliti; `OrderRepository.kt:72` `!!`; `CatalogRemoteMediator.kt:46-72` JSON xatosini crash qiladi; versiya hardcoded `1.0.0`.

---

## V. LOW (tanlab)

- `TODO/FIXME` — 0 ta (lekin test ham yo'q → tugallanmagan ish belgisi yo'q)
- Bo'sh catchlar: `AdminUserService.java:312,333`, `OperatingExpenseController.java:63`, `OrderService.java:368,570` (diskont release yutiladi), 11 ta JWT `ignored`
- `PricingDtos.of()` o'lik; `createdBy` to'ldirilmaydi; `recordEvent` xatosi `DeliveryService.create` ni rollback qilishi mumkin (flush commit'da)
- `docs/API-QULLANMA.md` da pricing bo'yicha umuman yozuv yo'q
- Desktop: `Sharhlar/Xarita` aralash til, `SUPER ADMIN PANEL` hardcoded, unused importlar
- Mobile: `tokenExpiresAt` yoziladi o'qilmaydi, splash 2200ms, versiya `1.0.0`, `home_delivery_address` soxta manzil, `strings.xml` o'lik

---

## VI. Test va sifat holati

| Xizmat | Test class | Xizmat | Test class |
|---|---|---|---|
| auth | 3 | **gateway** | **0** |
| product | 4 | **order** | **0** |
| warehouse | 1 | **delivery** | **0** |
| payment | 1 | **shop, discount, notification, analytics, geo, report, search, realtime-lib** | **0** |

- Jami: **9 test class / 26 test method**; 7 ta `pom.xml` da `spring-boot-starter-test` umuman yo'q
- **CI yo'q** (`.github/workflows` mavjud emas)
- Desktop/mobile'da faqat boilerplate testlar
- **Delivery pricing engine uchun 1 ta test ham yo'q** (kecha/thon qoidalari, clamp, threshold, rounding, timezone)

---

## VII. Yetishmayotgan qismlar (checklist)

**Xavfsizlik:** geo `/internal` secret · payment `/api` auth · product `anyRequest` yopish · `/internal/**` route'larni gateway'dan olib tashlash yoki majburiy secret · per-user SSE · sirlarni git'dan tozalash va rotatsiya · konfiguratsiyalangan CORS · Redis-based login rate-limit.

**Pul oqimi:** Payme refund/cancelTransaction · payment↔order reconciliation job · delivery FAILED/RETURNED → order holati + stok/diskont qaytarish · order status state machine · notification 404 ni tuzatish (`/internal/notifications/send`) · delivery yaratish/fee uchun retry/outbox.

**Pricing:** `pick()` 0-vs-null semantikasi · yozuvlarda `@Valid` + CHECK constraintlar · vaqt mintaqasi (`TZ`) va sana-schedule surcharge · UI↔engine surcharge turlarini moslash · `distanceKm` hisoblash (haversine) + `InternalDeliveryCreateRequest` ga `distanceKm/deliveryType` · event'da rule_id/surcharge breakdown · exposure tracking (quote darajasida) · indekslar · pricing'da test to'plami.

**Desktop:** couriers/deliveries/zone-create ekranlarini qaytarish (yoki navdan olib tashlab hujjatlash) · Versions/Audit tablari · delete reason · tozalanadigan maydonlar · haqiqiy paginatsiya (5 ro'yxat) · top search · SSE backoff · refresh xatosini logout dan ajratish · lokalizatsiya (96% hardcoded) · AddUserDialog · analytics/promotions/groups/search UI · per-tab loading.

**Mobile:** BASE_URL konfiguratsiyasi + HTTPS · checkout summasini tuzatish (zone/distance/discount/feeReady) · SSE auth header + response yopish · refresh xatosini logout dan ajratish · savat increment · server logout + session o'chirish · filter/sort · FCM · to'lov integratsiyasi · profil tahriri/delivery tracking/promo preview ekranlari.

**Repo:** 133 o'zgarishni commit qilish · `.cursor-tmp-flyway*` ni o'chirish · CI qo'shish · `docs/API-QULLANMA.md` ni yangilash.

---

## VIII. Tavsiya etilish tartibi

**P1 — xavfsizlik va pul (1–2 kun):** geo internal secret · payment `/api` auth · product `anyRequest().authenticated()` · `TZ=Asia/Tashkent` qo'shish · sirlarni git'dan olib tashlash · commit qilish.

**P2 — pul oqimi ishonchliligi (3–5 kun):** notification endpointni tuzatish · delivery FAILED/RETURNED → order sync · order status state machine · delivery/fee retry · payment↔order reconciliation · report-service Feign interceptor.

**P3 — pricing to'g'riligi (2–3 kun):** `pick()` semantikasi · validatsiya + CHECK · UI↔engine surcharge mosligi · `distanceKm` hisoblash · checkout/quote/order narxini birlashtirish · pricing testlari.

**P4 — UI to'liqligi (5–8 kun):** desktop couriers/deliveries qaytarish + Versions/Audit · desktop paginatsiya/backoff/localization · mobile checkout xatolari + SSE.

**P5 — sifat:** CI (build + test), test yozish (kamida pricing, auth security, order state machine), filtr/paginatsiya/lokalizatsiya qolgan ishlar.
