package mexa.club.authservice.config;

import mexa.club.authservice.security.JwtRequestFilter;
import mexa.club.authservice.security.SwaggerAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security konfiguratsiyasi.
 *
 * Himoya qatlami (defense-in-depth):
 *   1. SecurityConfig requestMatchers  — URL-darajasida permission tekshiruvi
 *   2. @PreAuthorize                   — metod-darajasida nozik tekshiruv (o'z-o'zini yangilash, va h.k.)
 *
 * Permission → Endpoint to'liq xaritalash:
 *
 *   USER_VIEW        → GET  /api/admin/users, /api/admin/users/by-email, /api/admin/users/{id}
 *   USER_CREATE      → POST /api/admin/users
 *   USER_EDIT        → PUT  /api/admin/users/{id}  |  POST .../block  |  POST .../unblock
 *   USER_DELETE      → DELETE /api/admin/users/{id}
 *   USER_ROLE_ASSIGN → PUT|POST /api/admin/users/{id}/roles
 *   ROLE_MANAGE      → /api/admin/roles/**
 *
 * SUPER_ADMIN barcha permission'larga ega (V7 migration), shuning uchun
 * har qanday hasAuthority('X') uchun avtomatik o'tadi.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final SwaggerAccessFilter swaggerAccessFilter;
    private final String corsAllowedOrigins;

    public SecurityConfig(
            JwtRequestFilter jwtRequestFilter,
            SwaggerAccessFilter swaggerAccessFilter,
            @Value("${spring.web.cors.allowed-origins}") String corsAllowedOrigins
    ) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.swaggerAccessFilter = swaggerAccessFilter;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz

                // ── Preflight ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Swagger / OpenAPI / Actuator ───────────────────────────────
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()

                // ── Ommaviy auth endpointlari ──────────────────────────────────
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/google",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/forgot-password",
                        "/api/auth/verify-reset-code",
                        "/api/auth/reset-password",
                        "/api/auth/verify-email",
                        "/api/auth/resend-code"
                ).permitAll()

                // ── Ichki servis-servis muloqoti (X-Internal-Secret) ─────────
                .requestMatchers("/internal/**").permitAll()

                // ── Sessiya boshqaruvi (o'z sessiyalari) ──────────────────────
                .requestMatchers("/api/auth/sessions/**").authenticated()

                // ── O'z profilini ko'rish va tahrirlash (har qanday autentifikatsiyalangan) ──
                .requestMatchers(HttpMethod.GET, "/api/admin/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/admin/users/me").authenticated()

                // ── O'z parolini o'zgartirish (@PreAuthorize self-check qo'shimcha) ─
                .requestMatchers(HttpMethod.PUT, "/api/admin/users/*/password").authenticated()

                // ─────────────────────────────────────────────────────────────
                // FOYDALANUVCHILAR BO'LIMI
                // Eng spesifik path'lar avval keladi
                // ─────────────────────────────────────────────────────────────

                // Foydalanuvchi qidirish email bo'yicha → USER_VIEW
                .requestMatchers(HttpMethod.GET, "/api/admin/users/by-email")
                        .hasAuthority("USER_VIEW")

                // Foydalanuvchi ro'yxatini ko'rish → USER_VIEW
                .requestMatchers(HttpMethod.GET, "/api/admin/users")
                        .hasAuthority("USER_VIEW")

                // Yangi foydalanuvchi yaratish → USER_CREATE
                .requestMatchers(HttpMethod.POST, "/api/admin/users")
                        .hasAuthority("USER_CREATE")

                // Foydalanuvchiga rol berish (replace) → USER_ROLE_ASSIGN
                // Additive (merge) uchun POST /api/admin/roles/assign dan foydalaning
                .requestMatchers(HttpMethod.PUT, "/api/admin/users/*/roles")
                        .hasAuthority("USER_ROLE_ASSIGN")

                // Foydalanuvchini bloklash / blokdan chiqarish → USER_EDIT
                .requestMatchers(HttpMethod.POST, "/api/admin/users/*/block")
                        .hasAuthority("USER_EDIT")
                .requestMatchers(HttpMethod.POST, "/api/admin/users/*/unblock")
                        .hasAuthority("USER_EDIT")

                // Foydalanuvchini ID bo'yicha ko'rish → USER_VIEW
                .requestMatchers(HttpMethod.GET, "/api/admin/users/*")
                        .hasAuthority("USER_VIEW")

                // Foydalanuvchini tahrirlash → USER_EDIT
                // (@PreAuthorize o'z profilini ham ruxsat beradi)
                .requestMatchers(HttpMethod.PUT, "/api/admin/users/*")
                        .hasAuthority("USER_EDIT")

                // Foydalanuvchini o'chirish → USER_DELETE
                .requestMatchers(HttpMethod.DELETE, "/api/admin/users/*")
                        .hasAuthority("USER_DELETE")

                // ─────────────────────────────────────────────────────────────
                // ROLLAR BO'LIMI  →  ROLE_MANAGE
                // ─────────────────────────────────────────────────────────────

                // Permission'larni kategoriya bo'yicha ko'rish
                .requestMatchers(HttpMethod.GET, "/api/admin/roles/permissions")
                        .hasAuthority("ROLE_MANAGE")

                // Rol ro'yxati
                .requestMatchers(HttpMethod.GET, "/api/admin/roles")
                        .hasAuthority("ROLE_MANAGE")

                // Yangi rol yaratish
                .requestMatchers(HttpMethod.POST, "/api/admin/roles")
                        .hasAuthority("ROLE_MANAGE")

                // Bitta rolni ko'rish / yangilash / o'chirish
                .requestMatchers(HttpMethod.GET,    "/api/admin/roles/*").hasAuthority("ROLE_MANAGE")
                .requestMatchers(HttpMethod.PUT,    "/api/admin/roles/*").hasAuthority("ROLE_MANAGE")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/roles/*").hasAuthority("ROLE_MANAGE")

                // Userga rol berish / olib tashlash
                .requestMatchers(HttpMethod.POST,   "/api/admin/roles/assign")
                        .hasAuthority("ROLE_MANAGE")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/roles/assign/*/*")
                        .hasAuthority("ROLE_MANAGE")

                // ─────────────────────────────────────────────────────────────
                // GURUHLAR BO'LIMI  →  GROUP_MANAGE
                // ─────────────────────────────────────────────────────────────

                .requestMatchers(HttpMethod.GET, "/api/admin/groups")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.POST, "/api/admin/groups")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.GET, "/api/admin/groups/*")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.PUT, "/api/admin/groups/*")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/groups/*")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.POST, "/api/admin/groups/*/members")
                        .hasAuthority("GROUP_MANAGE")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/groups/*/members/*")
                        .hasAuthority("GROUP_MANAGE")

                // ─────────────────────────────────────────────────────────────
                // Qolgan barcha so'rovlar — faqat autentifikatsiya talab qilinadi
                // ─────────────────────────────────────────────────────────────
                .anyRequest().authenticated()
            );

        http.addFilterBefore(swaggerAccessFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
