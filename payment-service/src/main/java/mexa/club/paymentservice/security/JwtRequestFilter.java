package mexa.club.paymentservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Authorization: Bearer <JWT> sarlavhasini tekshiradi va xavfsizlik kontekstini
 * to'ldiradi. (auth-service'da ishlab chiqilgan tokenlar, umumiy jwt.secret.)
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/internal/")
                || path.startsWith("/payment/payme/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(header.substring(7));
                UUID userId = jwtUtil.extractUserId(claims);
                String username = claims.getSubject();
                Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                Object roles = claims.get("roles");
                if (roles instanceof Iterable<?> iterable) {
                    for (Object role : iterable) {
                        authorities.add(new SimpleGrantedAuthority(String.valueOf(role)));
                    }
                }
                Object perms = claims.get("permissions");
                if (perms instanceof Iterable<?> permIterable) {
                    for (Object perm : permIterable) {
                        authorities.add(new SimpleGrantedAuthority(String.valueOf(perm)));
                    }
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new PaymentJwtPrincipal(userId, username), null, authorities));
            } catch (Exception ignored) {
                // Yaroqsiz token — authentication qoldirilmaydi, chain davom etadi
                // (anonim so'rov authenticated endpointda 401/403 oladi).
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /** To'liq profil ma'lumotini talab qilmaydigan yengil principal. */
    public record PaymentJwtPrincipal(UUID userId, String username) {
        @Override
        public String toString() {
            return username != null ? username : String.valueOf(userId);
        }
    }
}
