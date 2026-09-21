package mexa.club.shopservice.config;

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
    public OpenAPI shopOpenApi(
            @Value("${server.port:8094}") String serverPort
    ) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;
        String description = """
                **Mijozlar do'koni API** — mahsulotlar katalogi, savat, buyurtmalar, sharhlar va wishlist.

                | Yo'l prefiksi | Kim | Tavsif |
                |---|---|---|
                | `GET /api/products` | Ochiq | Mahsulotlar ro'yxati (filter, pagination) |
                | `GET /api/products/search` | Ochiq | Mahsulot qidirish |
                | `GET /api/products/{id}` | Ochiq | Mahsulot batafsil |
                | `GET /api/shops/products/{id}/reviews` | Ochiq | Mahsulot sharhlari |
                | `GET /api/shops/products/{id}/rating` | Ochiq | Reyting statistikasi |
                | `GET /api/shops/products/recommended` | Ochiq | Tavsiya etilgan mahsulotlar |
                | `GET/PUT/DELETE /api/shops/me/cart` | JWT | Savat boshqaruvi |
                | `PUT /api/shops/me/cart/items/{productId}` | JWT | Savat elementini qo'shish/yangilash |
                | `GET/POST/DELETE /api/shops/me/wishlist` | JWT | Wishlist |
                | `GET/POST/PUT/DELETE /api/shops/me/addresses` | JWT | Manzillar CRUD |
                | `PATCH /api/shops/me/addresses/{id}/default` | JWT | Default manzil belgilash |
                | `GET/POST /api/shops/me/orders` | JWT | Buyurtmalar ro'yxati va yaratish |
                | `GET /api/shops/me/orders/{id}` | JWT | Buyurtma batafsil |
                | `POST /api/shops/me/orders/{id}/cancel` | JWT | Buyurtmani bekor qilish |
                | `GET/POST /api/shops/me/reviews` | JWT | O'z sharhlari |
                | `GET /api/shops/admin/reviews` | `PRODUCT_MANAGE` | Admin: barcha sharhlar |
                | `DELETE /api/shops/admin/reviews/{reviewId}` | `PRODUCT_MANAGE` | Admin: sharh o'chirish |

                ### JWT
                Login javobida `token` keladi. **Authorize** da `Bearer <token>` kiriting.

                ### Tezkor havolalar
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/shop/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/shop/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (shop-service)")
                ))
                .info(new Info()
                        .title("Shop Service API")
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
