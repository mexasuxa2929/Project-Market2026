package mexa.club.authservice.config;

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
    public OpenAPI authOpenAPI(
            @Value("${server.port:8081}") String serverPort
    ) {
        final String bearerScheme = "bearerAuth";
        String baseUrl = "http://localhost:" + serverPort;

        String description = """
                **Autentifikatsiya va admin boshqaruv.**

                ### Ochiq endpointlar
                - `POST /api/auth/login`
                - `POST /api/auth/register` (ma’lumot `email_otp`da; verify dan keyin `users`; username users+faol OTP da tekshiriladi)
                - `POST /api/auth/verify-email` (6 raqamli OTP bilan tasdiqlash)
                - `POST /api/auth/resend-code` (yangi OTP; oldingi kod bekor)
                - `POST /api/auth/google` (Google idToken bilan login/register)

                ### Email OTP (xavfsizlik)
                Har bir kod **`java.security.SecureRandom`** yordamida **6 raqamli** (100000–999999) ixtiyoriy qiymat sifatida yaratiladi. Serverda kod **ochiq matn emas**, **SHA-256 hash** ko‘rinishida saqlanadi. Muddati, urinishlar va qayta yuborish oralig‘i **`app.email-otp.*`** (yoki `EMAIL_OTP_*` env) orqali: standart **TTL 300 s**, **maks. 5** noto‘g‘ri urinish, qayta yuborish oralig‘i **kamida 60 s**.

                ### JWT
                Login javobida `token` keladi. **Authorize** da `Bearer <token>` kiriting.
                Access token muddati `app.jwt.access-token-ttl` (default 900s), refresh token `app.jwt.refresh-token-ttl` (default 7 kun).

                ### Sessions API
                - `GET /api/auth/sessions` — faol sessionlar ro‘yxati
                - `DELETE /api/auth/sessions/{id}` — bitta sessionni yopish
                - `DELETE /api/auth/sessions` — barcha sessionlarni yopish

                ### Admin API (`/api/admin/**`)
                Barcha yo‘llar JWT talab qiladi. Test uchun: `superadmin` / `super123`, `superadmin2` / `super123`.

                | Metod | Yo‘l | Kim | Tavsif |
                |-------|------|-----|--------|
                | GET | `/api/admin/users` | SUPER_ADMIN | Ro‘yxat (pagination) |
                | POST | `/api/admin/users` | SUPER_ADMIN | OTP siz user yaratish (`AdminCreateUserRequest`) |
                | GET | `/api/admin/users/by-email?email=` | SUPER_ADMIN | Email bo‘yicha (trim, case-insensitive) |
                | GET | `/api/admin/users/{id}` | SUPER_ADMIN | ID bo‘yicha |
                | PUT | `/api/admin/users/{id}` | SUPER_ADMIN yoki **o‘zi** | Profil (`username`, `email`, `enabled`); **parol bu yerda emas** |
                | PUT | `/api/admin/users/{id}/password` | **Faqat o‘zi** | `oldPassword` + `newPassword` (SUPER_ADMIN ham boshqa user uchun emas) |
                | DELETE | `/api/admin/users/{id}` | SUPER_ADMIN | O‘chirish |
                | PUT | `/api/admin/users/{id}/roles` | SUPER_ADMIN | Body: **`roleIds`** — UUID massiv (bo‘sh bo‘lsa `ROLE_USER` tayinlanadi) |
                | GET | `/api/admin/roles` | SUPER_ADMIN | Barcha rollar |

                **`{id}`** — foydalanuvchi yoki rol **UUID** si (masalan `550e8400-e29b-41d4-a716-446655440000`).

                ### Pagination (`GET /api/admin/users`)
                `page` **0 dan** boshlanadi (`page=0` — birinchi sahifa). `totalPages=1` bo‘lsa `page=1` bo‘sh `items` qaytaradi.

                ### Tezkor havolalar (shu server)
                - **Swagger UI (local):** `%s/swagger-ui/index.html`
                - **OpenAPI JSON (local):** `%s/v3/api-docs`
                - **Swagger UI (gateway):** `http://localhost:8080/swagger/auth/swagger-ui/index.html`
                - **OpenAPI JSON (gateway):** `http://localhost:8080/swagger/auth/v3/api-docs`
                """.formatted(baseUrl, baseUrl);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url(baseUrl).description("Local (auth-service)")
                ))
                .info(new Info()
                        .title("Auth Service API")
                        .version("v1")
                        .description(description))
                .externalDocs(new ExternalDocumentation()
                        .description("Gateway — barcha servislar Swagger UI")
                        .url("http://localhost:8080/swagger-ui.html"))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name("Authorization")
                                .description("JWT: login javobidagi `token`")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
