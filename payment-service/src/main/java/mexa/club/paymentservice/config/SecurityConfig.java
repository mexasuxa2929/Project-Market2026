package mexa.club.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Payme Merchant API callback Basic Auth controller ichida tekshiriladi.
     * Production'da tavsiya: HTTPS + Payme IP whitelist (reverse proxy / firewall).
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/payment/payme/**", "/api/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated());
        return http.build();
    }
}
