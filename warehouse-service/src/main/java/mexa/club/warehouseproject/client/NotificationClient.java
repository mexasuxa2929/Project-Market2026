package mexa.club.warehouseproject.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.warehouseproject.exception.UpstreamReferenceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * notification-service'ga ichki ogohlantirishlarni yuborish (X-Internal-Secret bilan).
 */
@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalSecret;

    public NotificationClient(
            @Qualifier("notificationServiceRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${services.notification.url:${NOTIFICATION_SERVICE_URL:http://localhost:8087}}") String baseUrl,
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
     * Yagona foydalanuvchiga EMAIL orqali LOW_STOCK_ALERT yuboradi.
     *
     * @return true agar muvaffaqiyatli yuborilgan bo'lsa; service mavjud emas yoki rad etsa false.
     */
    public boolean sendLowStockAlert(UUID userId, String recipientEmail, Map<String, Object> variables) {
        if (userId == null) {
            return false;
        }
        if (baseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Notification service base URL is not configured (services.notification.url)");
        }
        String url = baseUrl + "/internal/notifications/send";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("userId", userId.toString());
        body.put("type", "LOW_STOCK_ALERT");
        body.put("channel", "EMAIL");
        body.put("recipientEmail", recipientEmail);
        body.put("variables", variables == null ? Map.of() : variables);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode success = root.get("success");
            return success != null && success.asBoolean(false);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Notification service response could not be read", e);
        } catch (Exception e) {
            throw new UpstreamReferenceException("Notification service is unavailable", e);
        }
    }
}
