package mexa.club.warehouseproject.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.warehouseproject.exception.UpstreamReferenceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * auth-service'ning ichki endpointidan foydalanuvchi email'larini olish (X-Internal-Secret bilan).
 */
@Component
public class AuthUserClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalSecret;

    public AuthUserClient(
            @Qualifier("authServiceRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${services.auth.url:${AUTH_SERVICE_URL:http://localhost:8081}}") String baseUrl,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.baseUrl = base;
        this.internalSecret = internalSecret;
    }

    /**
     * Berilgan user ID'lar uchun userId -> email xaritasini qaytaradi.
     * Topilmagan userlar yoki email'siz userlar xaritaga kirmaydi.
     */
    public Map<UUID, String> resolveEmails(Collection<UUID> userIds) {
        Map<UUID, String> result = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        if (baseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Auth service base URL is not configured (services.auth.url)");
        }
        List<String> ids = new ArrayList<>(userIds.stream().map(UUID::toString).toList());
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/internal/auth/users/by-ids")
                .queryParam("ids", String.join(",", ids))
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return result;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isObject()) {
                return result;
            }
            data.fields().forEachRemaining(entry -> {
                UUID id = safeUuid(entry.getKey());
                if (id == null || entry.getValue() == null || entry.getValue().isNull()) {
                    return;
                }
                String email = entry.getValue().asText();
                if (email != null && !email.isBlank()) {
                    result.put(id, email);
                }
            });
            return result;
        } catch (IOException e) {
            throw new UpstreamReferenceException("Auth service response could not be read", e);
        } catch (Exception e) {
            throw new UpstreamReferenceException("Auth service is unavailable", e);
        }
    }

    private static UUID safeUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
