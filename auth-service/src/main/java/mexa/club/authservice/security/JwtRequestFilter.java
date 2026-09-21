package mexa.club.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import mexa.club.authservice.service.TokenBlacklistService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtRequestFilter(
            UserDetailsService userDetailsService,
            JwtUtil jwtUtil,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Login/register va boshqa ochiq auth yo'llari uchun JWT filterni umuman ishga tushirmaymiz.
     * Spring Security 7 da filter ichida {@code AntPathRequestMatcher} tavsiya etilmaydi — servlet yo'li bilan tekshiramiz
     * ({@code SecurityConfig} dagi {@code requestMatchers("/api/auth/**")} bilan bir xil ma'noda).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicAuthPath(request);
    }

    /** {@code /api/auth} va {@code /api/auth/**} — boshqa {@code /api/auth...} bilan aralashmaydi. */
    private static boolean isPublicAuthPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path = path + request.getPathInfo();
        }
        if (path.isEmpty()) {
            path = "/";
        }
        if (path.startsWith("/internal/")) {
            return true;
        }
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/logout".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/verify-reset-code".equals(path)
                || "/api/auth/reset-password".equals(path)
                || "/api/auth/google".equals(path)
                || "/api/auth/verify-email".equals(path)
                || "/api/auth/resend-code".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7).trim();
            if (jwt.isEmpty()) {
                writeUnauthorized(response, "INVALID_TOKEN", "Bearer token is empty");
                return;
            }
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                writeUnauthorized(response, "TOKEN_BLACKLISTED", "Token is no longer valid");
                return;
            }
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                writeUnauthorized(response, "INVALID_TOKEN", "Invalid or malformed token");
                return;
            }
            if (username == null || username.isBlank()) {
                writeUnauthorized(response, "INVALID_TOKEN", "Token subject is missing");
                return;
            }
        }

        if (username != null && jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!userDetails.isEnabled()) {
                    writeUnauthorized(response, "ACCOUNT_DISABLED", "Hisob bloklangan. Admin bilan bog'laning.");
                    return;
                }
                if (!jwtUtil.validateToken(jwt, userDetails)) {
                    writeUnauthorized(response, "INVALID_TOKEN", "Token validation failed");
                    return;
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                writeUnauthorized(response, "INVALID_TOKEN", "Token validation failed");
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

