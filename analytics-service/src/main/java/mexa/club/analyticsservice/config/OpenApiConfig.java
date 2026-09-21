package mexa.club.analyticsservice.config;

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
    public OpenAPI analyticsOpenApi(@Value("${server.port:8093}") String serverPort) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;

        String description = """
                **Analytics va statistika API.**

                Barcha endpointlar JWT talab qiladi. Ruxsat: `ADMIN` yoki `SUPER_ADMIN`.

                ### Asosiy endpointlar
                - `GET /api/analytics/dashboard` — umumiy ko'rinish (cache yoki `?refresh=true`)
                - `GET /api/analytics/revenue-chart?period=` — daromad grafigi
                - `GET /api/analytics/order-breakdown?period=` — buyurtmalar holati
                - `GET /api/analytics/top-products?period=&limit=` — eng ko'p sotilgan mahsulotlar
                - `GET /api/analytics/stock-alerts?threshold=` — kam qoldiq ogohlantirishlari
                - `GET /api/analytics/user-growth?period=` — foydalanuvchilar o'sishi
                - `GET /api/analytics/discount-summary` — chegirma va promokodlar statistikasi

                ### Period qiymatlari
                `today` / `daily`, `week` / `weekly` / `7d` / `last_7_days`,
                `month` / `monthly` / `30d` / `last_30_days`,
                `quarterly` / `90d` / `last_90_days`,
                `year` / `yearly` / `this_year`, ISO sana (`2026-05-20`)

                ### Tezkor havolalar
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/analytics/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/analytics/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (analytics-service)")
                ))
                .info(new Info()
                        .title("Analytics Service API")
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
