package mexa.club.searchservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    public JwtRequestFilter(JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/internal/")
                || p.startsWith("/api/search/")
                || p.startsWith("/v3/api-docs")
                || p.startsWith("/swagger-ui")
                || p.startsWith("/actuator/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(header.substring(7));
                String username = claims.getSubject();
                UUID userId = jwtUtil.extractUserId(claims);
                if (userId == null) {
                    String sub = claims.getSubject();
                    if (sub != null) {
                        try { userId = UUID.fromString(sub); } catch (IllegalArgumentException ignored) {}
                    }
                }
                if (userId != null) {
                    try {
                        String blocked = stringRedisTemplate.opsForValue().get("blocked:" + userId);
                        if ("1".equals(blocked)) {
                            writeUnauthorized(res, "ACCOUNT_DISABLED", "Hisob bloklangan. Admin bilan bog'laning.");
                            return;
                        }
                    } catch (Exception redisEx) { writeUnauthorized(res, "AUTH_ERROR", "Xizmat vaqtincha mavjud emas"); return; }
                }
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
                        new UsernamePasswordAuthenticationToken(new JwtUserPrincipal(userId, username), null, authorities)
                );
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    private static void writeUnauthorized(HttpServletResponse response, String code, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeCode = code.replace("\"", "\\\"");
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"" + safeCode + "\",\"message\":\"" + safeMessage + "\"}}");
    }
}
