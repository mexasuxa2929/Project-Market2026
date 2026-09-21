package mexa.club.discountservice.security;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                UUID userId = jwtUtil.extractUserId(claims);
                if (userId == null) {
                    String sub = claims.getSubject();
                    if (sub != null) {
                        try { userId = UUID.fromString(sub); } catch (IllegalArgumentException ignored) {}
                    }
                }
                String username = claims.getSubject();
                if (userId != null) {
                    try {
                        String blocked = stringRedisTemplate.opsForValue().get("blocked:" + userId);
                        if ("1".equals(blocked)) {
                            writeUnauthorized(response, "ACCOUNT_DISABLED", "Hisob bloklangan. Admin bilan bog'laning.");
                            return;
                        }
                    } catch (Exception redisEx) { writeUnauthorized(response, "AUTH_ERROR", "Xizmat vaqtincha mavjud emas"); return; }
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
                JwtUserPrincipal principal = new JwtUserPrincipal(userId, username);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
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
