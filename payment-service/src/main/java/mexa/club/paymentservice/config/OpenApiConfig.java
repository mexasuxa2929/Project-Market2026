package mexa.club.paymentservice.config;

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
    public OpenAPI paymentOpenApi(@Value("${server.port:8091}") String serverPort) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;

        String description = """
                **To'lov servisi API.**

                | Yo'l prefiksi | Kim | Tavsif |
                |---|---|---|
                | `/api/payments/**` | JWT (har qanday rol) | Mijoz to'lovlari |
                | `/api/admin/payments/**` | `ADMIN` / `SUPER_ADMIN` | Admin boshqaruvi |
                | `/internal/payments/**` | `X-Internal-Secret` header | Servislar arasi |

                ### JWT
                Login javobida `token` keladi. **Authorize** da `Bearer <token>` kiriting.

                ### Tezkor havolalar
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/payment/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/payment/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (payment-service)")
                ))
                .info(new Info()
                        .title("Payment Service API")
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
