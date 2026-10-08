package mexa.club.gatewayservice.predicate;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ShortcutConfigurable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@Component
public class RoleRoutePredicateFactory extends AbstractRoutePredicateFactory<RoleRoutePredicateFactory.Config> {

    private final String jwtSecret;

    // H11: default yo'q — jwt.secret majburiy (yolgon zaif kalit bilan ishga tushmasligi uchun).
    public RoleRoutePredicateFactory(@Value("${jwt.secret}") String jwtSecret) {
        super(Config.class);
        this.jwtSecret = jwtSecret;
    }

    @Override
    public ShortcutConfigurable.ShortcutType shortcutType() {
        return ShortcutConfigurable.ShortcutType.GATHER_LIST;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("roles");
    }

    @Override
    public String name() {
        return "RoleRoute";
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                return false;
            }
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                // H7: RoleRoute qiymatlari role ("ROLE_ADMIN") yoki
                // "perm:PRODUCT_MANAGE" (ruxsat nomi) bo'lishi mumkin.
                List<String> required = config.roles;
                Collection<?> rolesCollection = asCollection(claims.get("roles"));
                Collection<?> permissionsCollection = asCollection(claims.get("permissions"));
                for (String req : required) {
                    if (req == null || req.isBlank()) {
                        continue;
                    }
                    String value = req.trim();
                    if (value.startsWith("perm:")) {
                        String permission = value.substring("perm:".length()).trim();
                        if (permissionsCollection != null && containsIgnoreCase(permissionsCollection, permission)) {
                            return true;
                        }
                    } else if (rolesCollection != null && containsIgnoreCase(rolesCollection, value)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception ex) {
                return false;
            }
        };
    }

    private static Collection<?> asCollection(Object value) {
        return value instanceof Collection<?> c ? c : null;
    }

    private static boolean containsIgnoreCase(Collection<?> collection, String wanted) {
        for (Object item : collection) {
            if (item != null && String.valueOf(item).trim().equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static class Config {
        private List<String> roles = new ArrayList<>();

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
