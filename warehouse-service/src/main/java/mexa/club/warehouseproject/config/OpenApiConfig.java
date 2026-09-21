package mexa.club.warehouseproject.config;

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
    public OpenAPI warehouseOpenAPI(
            @Value("${server.port:8084}") String serverPort,
            @Value("${spring.application.name:WarehouseProject}") String appName
    ) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;

        String description = """
                **Ombor (warehouse) REST API.**

                ### JWT
                1. Avval **Auth Service** da `POST /api/auth/login` orqali token oling.
                2. Shu sahifada **Authorize** tugmasini bosing va qiymat: `Bearer <token>` yoki faqat token (Swagger odatda `Bearer ` ni o‘zi qo‘shadi).

                ### Pagination
                `page` parametri **0 dan** boshlanadi (birinchi sahifa: `page=0`). `page=1` bo‘lsa va jami 1 sahifa bo‘lsa, ro‘yxat bo‘sh kelishi mumkin.

                ### Rollar
                Ayrim endpointlar `USER` / `ADMIN` / `SUPER_ADMIN` bilan cheklangan — 403 bo‘lsa token rollarini tekshiring.

                ### Identifikatorlar (UUID)
                Path va JSON dagi **id** maydonlari **UUID** formatida (`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`). Noto‘g‘ri format 400 berishi mumkin.

                ### Tezkor havolalar
                - **Swagger UI (local):** %s/swagger-ui/index.html
                - **OpenAPI JSON (local):** %s/v3/api-docs
                - **Swagger UI (gateway):** http://localhost:8080/swagger/warehouse/swagger-ui/index.html
                - **OpenAPI JSON (gateway):** http://localhost:8080/swagger/warehouse/v3/api-docs
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (" + appName + ")")
                ))
                .info(new Info()
                        .title("Warehouse API")
                        .version("v1")
                        .description(description))
                .externalDocs(new ExternalDocumentation()
                        .description("Auth Service — login va JWT")
                        .url("http://localhost:8081/swagger-ui/index.html"))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name("Authorization")
                                .description("JWT: `Bearer <token>` (auth-service login javobidagi `token` maydoni)")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
