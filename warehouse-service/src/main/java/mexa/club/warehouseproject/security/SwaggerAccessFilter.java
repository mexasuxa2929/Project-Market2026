package mexa.club.warehouseproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SwaggerAccessFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final String swaggerUsername;
    private final String swaggerPassword;

    public SwaggerAccessFilter(
            @Value("${app.swagger.basic-auth.enabled:${APP_SWAGGER_BASIC_AUTH_ENABLED:false}}") boolean enabled,
            @Value("${app.swagger.username:${APP_SWAGGER_USERNAME:swagger}}") String swaggerUsername,
            @Value("${app.swagger.password:${APP_SWAGGER_PASSWORD:change-me-now}}") String swaggerPassword
    ) {
        this.enabled = enabled;
        this.swaggerUsername = swaggerUsername;
        this.swaggerPassword = swaggerPassword;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Basic ")) {
            unauthorized(response);
            return;
        }

        String[] creds = decodeBasic(auth.substring(6));
        if (creds == null) {
            unauthorized(response);
            return;
        }

        if (!swaggerUsername.equals(creds[0]) || !swaggerPassword.equals(creds[1])) {
            unauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String[] decodeBasic(String b64) {
        try {
            String raw = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            int idx = raw.indexOf(':');
            if (idx < 0) {
                return null;
            }
            return new String[]{raw.substring(0, idx), raw.substring(idx + 1)};
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Swagger\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Swagger auth required\"}}");
    }
}

