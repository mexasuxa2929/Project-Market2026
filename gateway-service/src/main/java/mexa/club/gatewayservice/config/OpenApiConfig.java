package mexa.club.gatewayservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi(@Value("${server.port:8080}") String serverPort) {
        String base = "http://localhost:" + serverPort;
        String description = """
                **API Gateway** — tashqi REST va Swagger uchun yagona kirish nuqtasi.

                ### Swagger UI (barcha mikroservislar)
                - Asosiy sahifa: **%s/swagger-ui.html**
                - Yuqoridagi ro‘yxatdan servisni tanlang (auth, shop, product, …).
                - JWT bilan sinash: avval **auth** da login, keyin **Authorize** → `Bearer <token>`.

                ### REST (bazaviy URL)
                Barcha klient so‘rovlari: **`%s`** + marshrut (masalan `/api/auth/login`, `/api/shops/me/cart`).

                ### Real vaqt
                - `GET %s/api/realtime/stream` — SSE (Redis kanaliga bog‘liq).

                ### Batafsil qo‘llanma
                Loyiha ildizida **`docs/API-QULLANMA.md`** — servislar, portlar, gateway jadvali, typikal oqimlar.
                """.formatted(base + "/swagger-ui.html", base, base);

        return new OpenAPI()
                .info(new Info()
                        .title("Mexa — API Gateway")
                        .version("1.0")
                        .description(description))
                .externalDocs(new ExternalDocumentation()
                        .description("Loyiha bo‘yicha to‘liq qo‘llanma (repozitoriy: docs/API-QULLANMA.md)"));
    }
}
