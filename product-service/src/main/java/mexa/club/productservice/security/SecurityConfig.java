package mexa.club.productservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final SwaggerAccessFilter swaggerAccessFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter, SwaggerAccessFilter swaggerAccessFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.swaggerAccessFilter = swaggerAccessFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Error dispatch (sendError → /error) — aks holda 400/404 o'rniga 403
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Rasmlar ommaviy — token kerak emas
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        // Admin/ombor endpointlari — GET bo'lsa ham autentifikatsiya talab qilinadi
                        // (avval e'lon qilinadi: birinchi mos qoida g'olib)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/export",
                                "/api/products/{id}/stock-summary"
                        ).authenticated()
                        // Mahsulotlarni o'qish (GET) ommaviy — shop-service orqali keladi
                        // (batch: shop katalogi; full: warehouse S2S — ikkalasi ham anonymous bo'lishi mumkin)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products",
                                "/api/products/search",
                                "/api/products/tags",
                                "/api/products/batch",
                                "/api/products/by-barcode/**",
                                "/api/products/{id}",
                                "/api/products/{id}/price",
                                "/api/products/{id}/images",
                                "/api/products/{id}/colors",
                                "/api/products/{id}/siblings",
                                "/api/products/{id}/full"
                        ).permitAll()
                        // Kategoriya/Brend/Ishlab chiqaruvchi ma'lumotlari ommaviy
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/brands/**", "/api/manufacturers/**").permitAll()
                        // JWT kerak: product CRUD, kategoriya proxy (NON-GET)
                        .requestMatchers(
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/brands/**",
                                "/api/manufacturers/**"
                        ).authenticated()
                        // Yangi endpointlar default-ochiq bo'lmasin: qolgan hammasi JWT talab qiladi
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(swaggerAccessFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

