package mexa.club.warehouseproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            if (token.isEmpty()) {
                writeUnauthorized(response, "INVALID_TOKEN", "Bearer token is empty");
                return;
            }
            try {
                if (!jwtUtil.validateToken(token)) {
                    writeUnauthorized(response, "INVALID_TOKEN", "Token validation failed");
                    return;
                }
                String username = jwtUtil.extractUsername(token);
                if (username == null || username.isBlank()) {
                    writeUnauthorized(response, "INVALID_TOKEN", "Token subject is missing");
                    return;
                }
                var userId = jwtUtil.extractUserId(token);
                if (userId == null) {
                    String sub = jwtUtil.extractUsername(token);
                    if (sub != null) {
                        try { userId = UUID.fromString(sub); } catch (IllegalArgumentException ignored) {}
                    }
                }
                if (userId == null) {
                    writeUnauthorized(
                            response,
                            "ACCOUNT_INVALID",
                            "Token userId claim is missing or invalid");
                    return;
                }
                if (userId != null) {
                    try {
                        String blocked = stringRedisTemplate.opsForValue().get("blocked:" + userId);
                        if ("1".equals(blocked)) {
                            writeUnauthorized(response, "ACCOUNT_DISABLED", "Hisob bloklangan. Admin bilan bog'laning.");
                            return;
                        }
                    } catch (Exception redisEx) { writeUnauthorized(response, "AUTH_ERROR", "Xizmat vaqtincha mavjud emas"); return; }
                }
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Rollar va user identity to'g'ridan-to'g'ri JWT claim'laridan olinadi.
                    var authorities = jwtUtil.extractAuthorities(token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new JwtAuthDetails(userId, username));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                writeUnauthorized(response, "INVALID_TOKEN", "Invalid or malformed token");
                return;
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

