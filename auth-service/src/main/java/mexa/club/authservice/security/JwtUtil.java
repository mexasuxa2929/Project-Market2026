package mexa.club.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import mexa.club.authservice.config.JwtSessionProperties;
import mexa.club.authservice.entity.Permission;
import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    private final JwtSessionProperties jwtSessionProperties;

    public JwtUtil(JwtSessionProperties jwtSessionProperties) {
        this.jwtSessionProperties = jwtSessionProperties;
    }

    @PostConstruct
    private void initSigningKey() {
        if (secret.contains("default_dev_secret") || secret.contains("change-me") || secret.contains("min_32_chars")) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not set! " +
                    "Please set a strong, unique secret (at least 32 characters). " +
                    "Example: export JWT_SECRET='your-very-long-random-secret-key-here'");
        }
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

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
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
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // `warehouseproject` role tekshirishi uchun JWT ichiga role'larni ham qo'shamiz.
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("username", userDetails.getUsername());
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles() == null
                ? java.util.List.of()
                : user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        claims.put("userId", user.getId() != null ? user.getId().toString() : null);
        claims.put("username", user.getUsername());
        claims.put("permissions",
                user.getRoles() == null ? java.util.List.of() :
                user.getRoles().stream()
                        .filter(r -> r.getPermissions() != null)
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::name)
                        .distinct()
                        .collect(Collectors.toList())
        );
        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long expirationMs = Math.max(1, jwtSessionProperties.getAccessTokenTtl()) * 1000L;
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public long remainingValiditySeconds(String token) {
        Date exp = extractExpiration(token);
        long remainingMs = exp.getTime() - System.currentTimeMillis();
        return Math.max(0L, remainingMs / 1000L);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

