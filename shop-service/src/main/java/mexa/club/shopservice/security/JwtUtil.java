package mexa.club.shopservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    public List<String> extractPermissions(String token) {
        Object perms = extractAllClaims(token).get("permissions");
        if (perms instanceof List<?> list) {
            return list.stream()
                    .filter(p -> p instanceof String)
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    public UUID extractUserId(String token) {
        Object raw = extractAllClaims(token).get("userId");
        if (raw == null) return null;
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
