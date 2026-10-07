package mexa.club.paymentservice.config;

import mexa.club.paymentservice.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    /**
     * Payme Merchant API callback Basic Auth controller ichida tekshiriladi.
     * /api/** — JWT talab qiladi (oldin hamma narsa ochiq edi).
     * /internal/** — X-Internal-Secret controller ichida tekshiriladi.
     * Production'da tavsiya: HTTPS + Payme IP whitelist (reverse proxy / firewall).
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                // Error dispatch (sendError → /error) — aks holda 400/404 o'rniga 403
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/payment/payme/**", "/internal/**").permitAll()
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                        "/actuator/health", "/actuator/info").permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest()
                                .authenticated());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
