# payment-service (Payme / Paycom Merchant API)

Spring Boot 3 moduli: Payme JSON-RPC callback (`/payment/payme/callback`), buyurtmalar (`orders`) va `payme_transactions` jadvallari, checkout havolasi generatori.

## Talablar

- Java 17+
- PostgreSQL 16 (dev uchun `docker-compose` dagi `postgres` konteyneri)

## Ma'lumotlar bazasi

Yangi DB yarating (bir marta):

```sql
CREATE DATABASE payments_db;
```

So'ngra `.env` yoki muhit o'zgaruvchilari orqali ulanishni bering (namuna root `.env.example` ichida).

## Ishga tushirish (lokal)

```bash
cd payment-service
mvn spring-boot:run
```

Yoki rootdan:

```bash
mvn -pl payment-service spring-boot:run
```

Standart port: **8091**.

## Konfiguratsiya

`application.yml` ichidagi `payme.*` bloklari yoki muhit orqali:

| O'zgaruvchi | Ma'nosi |
|-------------|---------|
| `PAYME_MERCHANT_ID` | Kabinetdagi merchant ID |
| `PAYME_TEST_KEY` | Sandbox kalit |
| `PAYME_PROD_KEY` | Production kalit |
| `PAYME_ACTIVE_KEY` | `test` yoki `prod` — qaysi kalit tekshiriladi |
| `PAYME_CALLBACK_PATH` | Dokumentatsiya uchun eslatma (callback URL manzilingiz HTTPS bo‘lishi kerak) |

Basic Auth: login **har doim** `Paycom`, parol — tanlangan aktiv kalit.

## Sandbox sinovi

1. Buyurtma yarating va checkout URL oling:

```http
POST http://localhost:8091/api/orders
Content-Type: application/json

{"userId":1,"amountTiyin":100000,"returnUrl":"https://your-shop.example/callback"}
```

2. Payme kabinetida callback URL: `https://<sizning-domen>/payment/payme/callback`

3. JSON-RPC chaqiriqlar uchun Postman kollektsiyasi: `postman/Payme.postman_collection.json`

## Testlar

```bash
cd payment-service
mvn test
```

Integratsion testlar H2 xotirasida ishlaydi (`application-test.yml`).

## Production eslatmalari

- Faqat **HTTPS** orqali ochiq qiling; imkon bo‘lsa Payme manba IP larini reverse proxy da cheklang.
- `PAYME_ACTIVE_KEY=prod` bo‘lganda faqat production kalit qabul qilinadi.
