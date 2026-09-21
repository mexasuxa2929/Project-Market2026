# Mexa Warehouse / Market — API va mikroservislar qo‘llanmasi

Bu hujjat loyiha qanday ishlashini, qaysi servis qayerda turishini, Swagger orqali qanday sinashni va odatdagi integratsiya ketma-ketligini qisqa va aniq qilib beradi.

---

## 1. Arxitektura (qisqa)

- **API Gateway** (`gateway-service`, odatda port **8080**) — barcha tashqi REST so‘rovlar uchun yagona kirish nuqtasi. Marshrutlar orqali so‘rov tegishli mikroservisga yo‘naltiriladi.
- **Swagger UI** ham gatewayda joylashgan: bitta sahifadan pastdagi ro‘yxatdan har bir servisning OpenAPI hujjatini tanlash mumkin.
- **JWT** odatda `auth-service` orqali beriladi; himoyalangan endpointlarda header: `Authorization: Bearer <token>`.
- **Redis** — tezlik cheklovi (rate limit), real vaqt (SSE) va ayrim keshlar uchun.
- **PostgreSQL** — asosiy ma’lumotlar bazasi (servislar bo‘yicha alohida DB yoki umumiy `warehouse_db` — `docker-compose` va migratsiyalarga qarang).
- **MinIO** — mahsulot rasmlari (`product-service`).
- **Elasticsearch** — qidiruv indeksi (`search-service`).

---

## 2. Mahalliy ishga tushirish (Docker)

Asosiy infratuzilma:

```bash
docker compose up -d postgres redis minio elasticsearch
```

Keyin kerakli mikroservislarni IDEA yoki `mvn spring-boot:run` bilan ishga tushiring. Gateway ishlashi uchun **JWT_SECRET** barcha JWT tekshiruvchi servislarda bir xil bo‘lishi kerak (`.env` / muhit o‘zgaruvchilari).

`docker/init-db.sh` quyidagi qo‘shimcha bazalar yaratadi (agar kerak bo‘lsa): `products_db`, `payments_db`, `discounts_db`, `analytics_db`.

---

## 3. Swagger — qayerdan ochish

| Muhit | Manzil |
|--------|--------|
| **Barcha servislar (tavsiya)** | `http://localhost:8080/swagger-ui.html` |
| Real vaqt (SSE, gateway) | `GET http://localhost:8080/api/realtime/stream` (matn/event-stream; brauzerda EventSource) |

Swagger UI chap/yuqoridagi **dropdown**da servislar ro‘yxati bor (auth, product, shop, warehouse, order, …). Har biri o‘zining `v3/api-docs` manziliga proxy qilinadi:

- Masalan: `http://localhost:8080/swagger/auth/swagger-ui/index.html` yoki dropdown orqali.

**Muhim:** `shop` va boshqa servislarda JWT bilan sinash uchun avval **auth** swaggerida `login` / `register` qilib token oling, keyin **Authorize** tugmasida `Bearer <token>` ni kiriting.

---

## 4. Servislar, portlar va vazifalari

| Servis | Standart port | Asosiy vazifa |
|--------|---------------|----------------|
| gateway-service | 8080 | Marshrutlash, Swagger UI, SSE |
| auth-service | 8081 | Ro‘yxatdan o‘tish, login, JWT, admin foydalanuvchilar |
| shop-service | 8094 | Vitrina mahsulotlari, savat, buyurtma (mijoz oqimi), manzil, wishlist |
| product-service | 8083 | Mahsulot CRUD (admin), fayl/rasm |
| warehouse-service | 8084 | Ombor zaxiralari, audit |
| order-service | 8085 | Buyurtmalar |
| catalog (reference) | 8086 | Gateway: `/api/categories/**`, `/api/brands/**`, `/api/manufacturers/**` — alohida repoda bo‘lishi mumkin; `docker-compose` da `CATALOG_SERVICE_URL` |
| notification-service | 8087 | Bildirishnomalar |
| delivery-service | 8088 | Yetkazib berish |
| report-service | 8089 | Hisobotlar (ichki integratsiyalar) |
| search-service | 8090 | Qidiruv (ES) |
| payment-service | 8091 | To‘lov (Payme callback `/payment/**`) |
| discount-service | 8092 | Chegirmalar / aksiyalar |
| analytics-service | 8096 | Admin analitika |

---

## 5. Gateway orqali asosiy API guruhlari

Barcha URL lar **bazaviy**: `http://localhost:8080` (gateway).

| Guruh | Yo‘l (prefix) | Qayerga boradi (qisqa) |
|--------|----------------|-------------------------|
| Autentifikatsiya | `/api/auth/**` | auth-service |
| Admin (foydalanuvchi/rol) | `/api/admin/users/**`, `/api/admin/roles` | auth-service |
| Do‘kon (mijoz) | `/api/shops/**` | shop-service |
| Mahsulotlar | `/api/products/**` | **Admin** (`ROLE_ADMIN` / `ROLE_SUPER_ADMIN`) → product-service; aks holda → shop-service (vitrina) |
| Fayllar | `/api/files/**` | product-service |
| Katalog ma’lumotnomalari | `/api/categories/**`, `/api/brands/**`, `/api/manufacturers/**` | catalog (8086) |
| Ombor | `/api/warehouses/**`, `/api/audit-logs/**` | warehouse-service |
| Buyurtmalar | `/api/orders/**`, `/api/admin/orders/**` | order-service |
| Bildirishnomalar | `/api/notifications/**`, `/api/admin/notifications/**`, … | notification-service |
| Yetkazib berish | `/api/deliveries/**`, `/api/delivery/**`, … | delivery-service |
| Hisobotlar | `/api/reports/**` | report-service |
| Qidiruv | `/api/search/**` | search-service |
| Chegirmalar | `/api/discounts/**`, `/api/admin/promotions/**` | discount-service |
| Analitika | `/api/admin/analytics/**` | analytics-service |
| To‘lov | `/payment/**` | payment-service (callback yo‘llari) |

`/internal/**` yo‘llari odatda **tashqi mijoz uchun emas** — servislar o‘rtasidagi chaqiriqlar uchun; Swaggerda ko‘rinsa ham, productionda himoyalangan bo‘lishi kerak.

---

## 6. Typikal mijoz oqimi (qaysi API ketma-ket)

Quyidagi tartib mantiqiy bog‘liqliklarni ko‘rsatadi; aniq JSON maydonlari har bir servisning Swagger **Schemas** bo‘limida.

1. **Hisob**
   - `POST /api/auth/register` — email va boshlang‘ich ma’lumotlar.
   - `POST /api/auth/verify-email` — OTP bilan tasdiqlash (auth swaggerida batafsil).
   - `POST /api/auth/login` — javobda `token` (JWT).

2. **Katalog va mahsulot**
   - `GET /api/products` yoki `GET /api/products/{id}` — vitrina (shop-service orqali).
   - Ixtiyoriy: `GET /api/search/...` — qidiruv (search-service).

3. **Savat va manzil**
   - `GET/POST .../api/shops/me/cart` — savat (shop swagger: `MeCartController`).
   - Manzil: `/api/shops/me/addresses` (GET/POST).

4. **Buyurtma**
   - Shop kontekstida: `POST /api/shops/me/orders` (yoki loyihadagi aniq endpoint — **shop-service** Swaggerdan tekshiring).

5. **To‘lov**
   - Payment swagger va `/payment/**` — Payme oqimi loyiha sozlamalariga (`PAYME_*`) bog‘liq.

6. **Real vaqt**
   - Frontend: `EventSource` bilan `GET /api/realtime/stream` — Redis kanalidan voqealar (mavjud bo‘lsa).

---

## 7. Admin / superadmin oqimi

- **Rollar va foydalanuvchilar:** `auth-service` — `/api/admin/users`, `/api/admin/roles` (Swaggerda jadval ko‘rinishida ham bor).
- **Mahsulot yaratish/tahrirlash:** `product-service` — gatewayda **`/api/products/**` + admin roli** sharti bilan product-service ga boradi. Oldindan katalog (kategoriya, brend) ma’lumotlari bo‘lishi kerak bo‘lishi mumkin — `product-service` integratsiyasiga qarang.
- **Ombor:** `/api/warehouses/**` — warehouse-service.
- **Chegirma:** `/api/admin/promotions/**` — discount-service.
- **Analitika:** `/api/admin/analytics/**` — analytics-service.

Standart test superadmin (auth swagger tavsiyasi): `superadmin` / `super123` (faqat dev muhit).

---

## 8. Oldindan mavjud bo‘lishi kerak bo‘lgan “field” va ma’lumotlar

Bu yerda “field” deganda **ma’lumotlar bazasi va biznes obyektlari** tushuniladi:

| Nima | Qachon kerak |
|------|----------------|
| **JWT_SECRET** | Barcha token tekshiradigan servislar bir xil qiymat |
| **APP_INTERNAL_SECRET** (yoki mos nom) | Servislar o‘rtasidagi ichki chaqiriqlar — `docker-compose` dagi default bilan sinash mumkin, productionda almashtiring |
| **PostgreSQL jadvallar** | Flyway migratsiyalar servis ishga tushganda yaratadi; DB URL to‘g‘ri bo‘lishi kerak |
| **Redis** | Gateway rate limit va SSE uchun; ayrim servislar uchun majburiy |
| **MinIO** | Mahsulot rasmlari yuklash uchun product-service |
| **Elasticsearch** | search-service uchun |
| **Foydalanuvchi roli** | Admin endpointlar uchun JWT ichida `ROLE_ADMIN` / `ROLE_SUPER_ADMIN` |
| **Mahsulot (SKU), kategoriya** | Buyurtma/savat uchun vitrinada ko‘rinadigan mahsulotlar |

---

## 9. Xavfsizlik va sinov

- Swagger **Authorize** — `Bearer` sxemasi.
- Rate limit gatewayda Redis bilan bog‘liq; Redis o‘chiq bo‘lsa, loyiha sozlamasiga qarab xatolik yoki cheklovsiz ishlash mumkin.
- **Ishlab chiqarishda** ichki (`/internal/**`) yo‘llarni tashqaridan yopish, secretlarni muhit o‘zgaruvchilari bilan berish shart.

---

## 10. Qayerdan chuqurroq o‘qish

- Har bir servisning o‘z **`OpenApiConfig`** yoki `application.yml` → `springdoc` qismidagi tavsifalar (masalan `auth-service` — OTP, admin jadvali).
- Ushbu fayl: **`docs/API-QULLANMA.md`** — loyiha bo‘yicha umumiy qo‘llanma.

---

## 11. Servislar holati (dashboard): UP, DOWN, 403, INTERNAL_ERROR

| Ko‘rinish | Ma’nosi | Nima qilish |
|------------|---------|-------------|
| **DOWN** | Odatda `localhost:PORT` da **hech narsa ishlamayapti** (servis ishga tushirilmagan yoki port boshqacha). | IDEA **Services** dan `shop-service`, `product-service`, `warehouse-service` ni ishga tushiring; Postgres, Redis, (product uchun) MinIO ishlayotganini tekshiring. |
| **Health DOWN, lekin API 200** | `/actuator/health` **HTTP 200** qaytaradi, lekin JSON ichida `"status":"DOWN"` (masalan Redis/DB indikatori). | Redis ishlamasa ham Swagger ochilishi mumkin — `management.health.redis.enabled: false` (loyihada bir qator servislar uchun qo‘llangan) yoki Redisni yoqing. |
| **Ishlayapti (actuator 403)** | Servis jonli, lekin `/actuator/health` yoki boshqa actuator yo‘li **security** tufayli 403 qaytaryapti; ba’zi monitoringlar buni “ogohlantirish” deb belgilaydi. | Agar faqat health ko‘rinsa — `SecurityConfig` da `/actuator/**` uchun `permitAll` va `spring-boot-starter-actuator` qo‘shilganini tekshiring (delivery/report/search/analytics uchun qo‘shildi). |
| **INTERNAL_ERROR** | Monitoring **HTTP 500** yoki API javobidagi `INTERNAL_ERROR` kodini ko‘rdi — sabab odatda **PostgreSQL ulanmadi**, **Flyway** xatosi yoki **Redis** yo‘q. | Servis **log** ini oching. Mahalliy ishda `order-service` uchun `orders_db`, `notification-service` uchun `notifications_db` yaratilgan-yo‘qligini tekshiring (`application.yml` dagi izohlar). Docker ishlatilsa, `docker-compose` odatda `warehouse_db` ga ulanadi. |

**Gateway Swagger** alohida ishlaydi: boshqa servislar o‘chiq bo‘lsa ham `http://localhost:8080/swagger-ui.html` ochilishi mumkin — bu “barcha mikroservislar ishlayapti” degani emas.

Savol yoki yangi endpoint qo‘shilganda avval **gateway** `application.yml` dagi `spring.cloud.gateway.routes` ni yangilanganini tekshiring.
