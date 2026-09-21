package mexa.club.productservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productOpenApi(
            @Value("${server.port:8083}") String serverPort
    ) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;
        String description = """
                **Mahsulotlar boshqaruvi API** — mahsulotlar, ranglar (color group), narxlar, kategoriyalar, brendlar va ishlab chiqaruvchilar.

                ### Mahsulotlar (`/api/products`)
                | Metod | Yo'l | Ruxsat | Tavsif |
                |---|---|---|---|
                | GET | `/api/products` | `PRODUCT_VIEW` | Ro'yxat (filter, pagination) |
                | GET | `/api/products/{id}` | `PRODUCT_VIEW` | Batafsil |
                | GET | `/api/products/{id}/full` | `PRODUCT_VIEW` | To'liq (narx, rangdosh mahsulotlar, omborxona) |
                | GET | `/api/products/search` | `PRODUCT_VIEW` | Qidiruv |
                | GET | `/api/products/tags` | `PRODUCT_VIEW` | Barcha teglar |
                | GET | `/api/products/by-barcode/{barcode}` | `PRODUCT_VIEW` | Barcode bo'yicha |
                | GET | `/api/products/export` | `PRODUCT_MANAGE` | Excel/CSV eksport |
                | POST | `/api/products` (JSON) | `PRODUCT_MANAGE` | Yangi mahsulot |
                | POST | `/api/products` (multipart) | `PRODUCT_MANAGE` | Mahsulot + rasmlar |
                | PUT | `/api/products/{id}` | `PRODUCT_MANAGE` | Yangilash |
                | PATCH | `/api/products/{id}/active` | `PRODUCT_MANAGE` | Faollik holati |
                | DELETE | `/api/products/{id}` | `PRODUCT_MANAGE` | O'chirish |

                ### Mahsulot rasmlari
                | Metod | Yo'l | Tavsif |
                |---|---|---|
                | GET | `/api/products/{id}/images` | Rasm ro'yxati |
                | POST | `/api/products/{id}/images` | Rasm yuklash |
                | DELETE | `/api/products/{id}/images/{filename}` | Rasm o'chirish |
                | PUT | `/api/products/{id}/images/reorder` | Tartibni o'zgartirish |

                ### Ranglar (color group), narxlar, narx darajalari
                - `GET /api/products/{id}/colors` — bir guruhdagi (group_id) barcha rang nusxalari
                - `POST /api/products/{id}/colors` — mavjud mahsulotga yangi rang (mustaqil Product, o'z barcode/narxi) qo'shish
                - `GET/POST/PUT/DELETE /api/products/{id}/prices` — narxlar tarixi
                - `GET/POST/PUT/DELETE /api/products/{id}/price-tiers` — ulgurji narx darajalari
                - `GET /api/products/{id}/stock-summary` — omborxona qoldig'i

                ### Katalog (kategoriyalar, brendlar, ishlab chiqaruvchilar)
                - `GET/POST/PUT/DELETE /api/categories` — kategoriyalar CRUD
                - `GET /api/categories/roots` — yuqori darajali kategoriyalar
                - `POST /api/categories/{id}/image` — kategoriya rasmi
                - `GET/POST/PUT/DELETE /api/brands` — brendlar CRUD
                - `POST /api/brands/{brandId}/logo` — brend logosi
                - `GET/POST/PUT/DELETE /api/manufacturers` — ishlab chiqaruvchilar CRUD

                ### Fayl xizmati (`/api/files`)
                - `GET /api/files/images/products/{productId}/{filename}` — mahsulot rasmi
                - `GET /api/files/images/brands/{brandId}/{filename}` — brend logosi
                - `GET /api/files/images/categories/{categoryId}/{filename}` — kategoriya rasmi

                ### JWT
                Login javobida `token` keladi. **Authorize** da `Bearer <token>` kiriting.

                ### Tezkor havolalar
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/product/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/product/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (product-service)")
                ))
                .info(new Info()
                        .title("Product Service API")
                        .version("v1")
                        .description(description))
                .externalDocs(new ExternalDocumentation()
                        .description("Auth Service — login va JWT")
                        .url("http://localhost:8081/swagger-ui/index.html"))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name("Authorization")
                                .description("Auth-service login javobidagi JWT tokenni `Bearer <token>` formatda yuboring.")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
