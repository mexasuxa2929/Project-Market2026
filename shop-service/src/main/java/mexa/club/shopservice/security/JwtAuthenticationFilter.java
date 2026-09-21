package mexa.club.shopservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import mexa.club.shopservice.security.JwtUserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeaderTrim = authHeader.trim();
        if (!authHeaderTrim.toLowerCase(Locale.ROOT).startsWith("bearer")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Supports both "Bearer <token>" and "Bearer<token>".
        String token = authHeaderTrim.substring(6).trim();
        if (token.isBlank()) {
            writeUnauthorized(response, "INVALID_TOKEN", "Bearer token is empty");
            return;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || username.isBlank()) {
                writeUnauthorized(response, "INVALID_TOKEN", "Token subject is missing");
                return;
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            jwtUtil.extractRoles(token).stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            jwtUtil.extractPermissions(token).stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);

            UUID userId = jwtUtil.extractUserId(token);
            JwtUserPrincipal principal = new JwtUserPrincipal(userId, username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            writeUnauthorized(response, "INVALID_TOKEN", "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
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
