package mexa.club.discountservice.config;

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
    public OpenAPI discountOpenApi(@Value("${server.port:8092}") String serverPort) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;
        String description = """
                **Promosiya va chegirmalar servisi** — kuponlar, promokodlar va chegirma qoidalari.

                | Yo'l prefiksi | Kim | Tavsif |
                |---|---|---|
                | `GET /api/discounts/active` | JWT (har kim) | Faol promosiyalar ro'yxati |
                | `POST /api/discounts/apply` | JWT (har kim) | Promokod qo'llash (tekshirish) |
                | `POST /api/admin/promotions` | `ADMIN` / `SUPER_ADMIN` | Yangi promosiya yaratish |
                | `GET /api/admin/promotions` | `ADMIN` / `SUPER_ADMIN` | Barcha promosiyalar (pagination) |
                | `GET /api/admin/promotions/stats` | `ADMIN` / `SUPER_ADMIN` | Promosiya statistikasi |
                | `GET /api/admin/promotions/{id}` | `ADMIN` / `SUPER_ADMIN` | Promosiya batafsil |
                | `PUT /api/admin/promotions/{id}` | `ADMIN` / `SUPER_ADMIN` | Promosiyani yangilash |
                | `DELETE /api/admin/promotions/{id}` | `ADMIN` / `SUPER_ADMIN` | Promosiyani o'chirish |
                | `GET /api/admin/promotions/{id}/usages` | `ADMIN` / `SUPER_ADMIN` | Foydalanish tarixi |
                | `POST /api/admin/promotions/{id}/deactivate` | `ADMIN` / `SUPER_ADMIN` | Promosiyani o'chirish |
                | `/internal/discounts/**` | `X-Internal-Secret` header | Servislar arasi (order-service) |

                ### Ichki endpointlar (`/internal/discounts/**`)
                Order-service tomonidan ishlatiladi. Har bir so'rovda `X-Internal-Secret` header bo'lishi shart.
                - `GET /internal/discounts/stats` — chegirma statistikasi
                - `POST /internal/discounts/apply` — chegirmani qo'llash va zaxiralash
                - `POST /internal/discounts/confirm` — buyurtma tasdiqlangandan so'ng chegirmani tasdiqlash
                - `POST /internal/discounts/cancel/{orderId}` — bekor qilingan buyurtma uchun chegirmani qaytarish

                ### JWT
                Login javobida `token` keladi. **Authorize** da `Bearer <token>` kiriting.

                ### Tezkor havolalar
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/discount/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/discount/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(new Server().url(baseUrl).description("Local (discount-service)")))
                .info(new Info().title("Discount Service API").version("v1").description(description))
                .externalDocs(new ExternalDocumentation()
                        .description("Auth Service — login va JWT")
                        .url("http://localhost:8081/swagger-ui/index.html"))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components().addSecuritySchemes(bearerScheme,
                        new SecurityScheme()
                                .name("Authorization")
                                .description("Auth-service login javobidagi JWT tokenni `Bearer <token>` formatda yuboring.")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
