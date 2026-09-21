# Discount Service

**IntelliJ / IDEA:** root `pom.xml` ustidan **Maven → Reload Project** (yoki `discount-service/pom.xml` → *Add as Maven Project*) — shundan keyin **Services** oynasida `DiscountServiceApplication` chiqadi. Loyihada `.run/DiscountServiceApplication.run.xml` qo‘shilgan.

Port **8092**. PostgreSQL database **`discounts_db`** (create manually before first run):

```sql
CREATE DATABASE discounts_db;
```

Stack: Spring Boot 3.x, JPA, Flyway, OpenFeign → product-service (JWT forwarded), JWT guard for public/admin APIs, internal endpoints protected by **`X-Internal-Secret`** (same `APP_INTERNAL_SECRET` as other services).

## Example: create a coupon (admin JWT)

```bash
curl -s -X POST "http://localhost:8092/api/admin/promotions" ^
  -H "Authorization: Bearer <ADMIN_JWT>" ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"SUMMER20\",\"name\":\"Summer 20%\",\"description\":\"Seasonal\",\"type\":\"PERCENTAGE\",\"value\":20,\"minOrderAmount\":100000,\"maxDiscountAmount\":500000,\"usageLimit\":1000,\"perUserLimit\":1,\"startsAt\":\"2026-06-01T00:00:00\",\"endsAt\":\"2026-09-01T23:59:59\",\"active\":true,\"restrictedProductIds\":[],\"restrictedCategoryIds\":[]}"
```

(Gateway: prefix path with gateway host and route, e.g. `http://localhost:8080/api/admin/promotions` with the same header.)

## Example: preview discount (customer JWT)

```bash
curl -s -X POST "http://localhost:8092/api/discounts/apply" ^
  -H "Authorization: Bearer <USER_JWT>" ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"summer20\",\"orderAmount\":250000,\"productIds\":[\"550e8400-e29b-41d4-a716-446655440001\"],\"categoryIds\":[]}"
```

Response includes `discountAmount`, `finalAmount`, and `freeShipping` when type is `FREE_SHIPPING`.

## Order-service integration (text diagram)

```
Client                    Gateway / Order-service              Discount-service
  |                                |                                  |
  |-- POST /api/orders + discountCode -->                         |
  |                                |-- Feign POST /internal/discounts/apply
  |                                |   (JWT + X-Internal-Secret) ---->|
  |                                |<----- discountAmount ----------|
  |                                | (persist order with discount fields)
  |                                |                                  |
  |                                |-- on payment OK: POST /internal/discounts/confirm
  |                                |-- on cancel / payment fail: POST /internal/discounts/cancel/{orderId}
```

Notes:

- **`internal/discounts/apply`** records **`promotion_usages`** and increments **`usage_count`** atomically (row lock on promotion).
- **`internal/discounts/confirm`** verifies the usage row matches order/user/code/amount (idempotent safety).
- **`internal/discounts/cancel`** rolls back usage for an order when the order is cancelled or payment fails after reservation.

Swagger UI (gateway aggregate): `http://localhost:8080/swagger-ui.html` → choose **discount**.
