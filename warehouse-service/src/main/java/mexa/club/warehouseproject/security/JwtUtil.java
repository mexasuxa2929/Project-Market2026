package mexa.club.warehouseproject.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    @PostConstruct
    private void initSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String raw = extractClaim(token, claims -> claims.get("userId", String.class));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return parseClaims(token).getBody();
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    /**
     * HS256 tokenni bir xil secret (UTF-8) bilan tekshiradi.
     */
    public boolean validateToken(String token) {
        Jws<Claims> jws = parseClaims(token);
        String algorithm = jws.getHeader().getAlgorithm();
        if (!"HS256".equalsIgnoreCase(algorithm)) {
            return false;
        }
        return !isTokenExpired(token);
    }

    /**
     * `auth-service` JwtUtil'dagi claim: "roles": [ "ROLE_ADMIN", "ROLE_USER", ... ]
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // roles
        Object rolesObj = extractClaim(token, claims -> claims.get("roles"));
        if (rolesObj instanceof List<?> rolesList) {
            rolesList.stream()
                    .filter(r -> r instanceof String)
                    .map(r -> ((String) r).trim())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // permissions — absent in old tokens: safe to skip
        Object permsObj = extractClaim(token, claims -> claims.get("permissions"));
        if (permsObj instanceof List<?> permsList) {
            permsList.stream()
                    .filter(p -> p instanceof String)
                    .map(p -> (String) p)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }
}

