# Production readiness review — Warehouse + Auth microservices

Senior backend / architect notes: **critical findings**, **changes applied in repo**, and **recommended next steps**.

---

## 1. Security — critical issues & mitigations

| Issue | Severity | Status / action |
|-------|----------|-----------------|
| **JWT secret in source control** | Critical | Mitigated: `JWT_SECRET`, DB creds use **environment variables** with defaults in `application.properties` / `application.yml`. **Remove defaults in prod** and inject only via secrets manager. |
| **Shared symmetric JWT** | Medium | HS256 + shared `jwt.secret` between services works for small setups. For scale: **RSA (asymmetric)** keys, JWKS endpoint from auth-service, or **opaque tokens** + introspection. |
| **Warehouse trusts claims without server-side user check** | Medium | Warehouse builds authorities **only from JWT**. If a token is leaked, it is valid until expiry. Mitigations: short TTL, refresh tokens, revocation list / Redis denylist, or gateway introspection. |
| **No rate limiting / brute-force on login** | High | **Not implemented.** Add bucket per IP (Bucket4j, Redis) or API Gateway limits. |
| **CORS** | Medium | Auth-service: explicit origins (good). Replicate for Warehouse if browsers call it directly. |
| **SQL injection** | Low (JPA) | Parameterized queries via Spring Data JPA + Specifications — **no raw concatenated SQL**. Keep avoiding native queries with string concat. |
| **Mass assignment** | Was Medium | Addressed earlier: `ProductRequest` DTO, `active` not updated via PUT, separate PATCH. |
| **Field injection** | Low | Replaced with **constructor injection** in key Warehouse + Auth classes (easier testing, immutable deps). |

### API Gateway (when you need it)

Use **Spring Cloud Gateway** (or Kong/Traefik) when:

- You want **one TLS entry**, WAF, rate limits, and **central JWT validation** before routing to services.
- You have **>2–3** public HTTP services or need **BFF** patterns.

Until then, **per-service JWT filter** (your current approach) is valid **Variant 1** from microservice literature.

---

## 2. Architecture — changes applied

| Area | Implementation |
|------|----------------|
| **DTOs vs entities (read path)** | `ProductResponse` for API; list/detail return **no JPA entity** JSON. |
| **Controller → Service → Repository** | Preserved; `ProductService` owns transactions + mapping. |
| **Flexible product search** | `ProductSpecification` + optional `name` (LIKE, case-insensitive) + `barcode` (exact) on **GET `/api/products`**. Legacy **GET `/api/products/search`** kept with stricter “at least one param” rule. |
| **Pagination** | **GET `/api/products?page=&size=&sort=`** (defaults: size 20, sort `name`). Max page size 200 in config. |

### Microservice communication

- Today: **sync HTTP** + duplicate JWT validation.  
- Next: **async events** (e.g. `ProductCreated`) via Kafka/RabbitMQ for inventory, search index, notifications; **OpenFeign** only if you need sync orchestration (prefer events for warehouse domains).

---

## 3. Code quality — applied

- **Constructor injection**: `ProductController`, `ProductService`, `SecurityConfig`, `JwtRequestFilter` (Warehouse); `AuthController`, `SecurityConfig`, `JwtRequestFilter`, `UserDetailsServiceImpl`, `DataInitializer` (Auth).
- **Validation**: Warehouse `ProductRequest` + `@Valid`; Auth `LoginRequest` / `RegisterRequest` + `spring-boot-starter-validation`.
- **Redundant DB calls**: `deleteProduct` uses **`existsById`** then `deleteById` (still two round-trips; optional single `deleteById` + check rows — DB-specific).

---

## 4. Error handling — applied

### Warehouse

- `@RestControllerAdvice` **`GlobalExceptionHandler`**: `ResourceNotFoundException`, `MethodArgumentNotValidException`, `AccessDeniedException`, generic `Exception`.
- Standard body: **`ApiResponse<T>`** with `success`, `data`, `error` (`code`, `message`, optional `fieldErrors`).

### Auth-service

- **`AuthExceptionHandler`**: `BadCredentialsException` → **401**, validation → **400**, generic **500**.
- Register conflicts → **409** with JSON from controller.
- Login response: `success` + `data.token` + root **`token`** for backward-compatible scripts.

---

## 5. Performance — applied & ideas

| Done | Idea |
|------|------|
| Pagination + sort on product list | **Indexes** on `product(name)`, `product(barcode)` in DB for search & uniqueness. |
| JPA Specifications for filters | **Read replicas** if reporting/search load grows. |
| — | **Caching**: product-by-id with Caffeine + TTL; **invalidate** on PUT/PATCH/DELETE. |
| — | **Spring Cache** `@Cacheable` on `getProductById` after adding indexes. |

---

## 6. Production readiness

### Config & secrets (done)

- Warehouse: `SPRING_DATASOURCE_*`, `JWT_SECRET`, `JWT_EXPIRATION_MS`.
- Auth: same + `app.cors.allowed-origins` still in YAML (move to env in prod).

### Logging (done — baseline)

- Warehouse `ProductService`: **info** on create/update/delete.
- Package log levels in `application.properties`.

### Docker (suggested)

Multi-stage build per service (example pattern):

```dockerfile
# warehouse/Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/WarehouseProject-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Run with `-e JWT_SECRET=... -e SPRING_DATASOURCE_URL=...`. Use **docker-compose** for Postgres + both apps + network.

---

## 7. Missing features for a real warehouse / marketplace

- **Inventory / reservations**: `WarehouseStock`, stock movements, optimistic locking.
- **Pricing**: `ProductPrice` with history, VAT, promos.
- **Auditing**: `createdBy`, `updatedBy`, `createdAt` (`@EntityListeners`), **Spring Data JPA Auditing**.
- **Idempotency** on POST (client `Idempotency-Key` header).
- **OpenAPI** (springdoc) + contract tests.
- **Observability**: Micrometer + Prometheus, trace IDs (Micrometer Tracing).
- **Outbox pattern** for reliable integration events.

---

## 8. Breaking API changes (Warehouse)

- List/search responses are now **`ApiResponse<PagePayload<ProductResponse>>`** (not raw `List<Product>`).
- Single product: **`ApiResponse<ProductResponse>`**.
- **DELETE** remains **204 No Content** (no JSON body).

Update Postman / frontend accordingly.

---

## 9. Step-by-step backlog (priority order)

1. Add **DB indexes** + integration tests for security (roles) and pagination.  
2. **Rotate JWT secret** in prod; enforce **HTTPS** only.  
3. Add **login rate limiting** + optional **account lockout**.  
4. Introduce **shared `common-security` / `common-api` JAR** or parent BOM to dedupe `ApiResponse` / JWT parsing.  
5. **Gateway** when third service appears or central policy is required.  
6. **RSA JWT** or **OAuth2 Resource Server** if partners integrate.

---

*This document reflects the state of the repository after the applied refactor pass.*
