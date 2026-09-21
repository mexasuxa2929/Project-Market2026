package mexa.club.productservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import mexa.club.productservice.exception.UpstreamReferenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Order-service ga mahsulot o'chirilishi oldidan bog'liq orderlar borligini so'rash uchun.
 */
@Component
public class OrderUsageClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final String orderBaseUrl;
    private final String internalSecret;

    public OrderUsageClient(
            @Value("${app.order-service.url:${ORDER_SERVICE_URL:http://localhost:8085}}") String orderBaseUrl,
            @Value("${app.internal-secret:change-me-internal}") String internalSecret
    ) {
        String base = orderBaseUrl == null ? "" : orderBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.orderBaseUrl = base;
        this.internalSecret = internalSecret;
    }

    /** Mahsulotga bog'liq orderlar soni (0 bo'lsa xavfsiz o'chirish mumkin). */
    public long countOrdersByProduct(UUID productId) {
        if (productId == null || orderBaseUrl.isEmpty()) {
            return 0;
        }
        String url = orderBaseUrl + "/internal/orders/usage/product/" + productId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-Internal-Secret", internalSecret)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new UpstreamReferenceException("Order usage check failed with status " + status, null);
            }
            return parseOrderCount(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Order usage check interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Order service is unavailable", e);
        }
    }

    private long parseOrderCount(String body) {
        if (body == null || body.isBlank()) {
            return 0;
        }
        // Eng oddiy: "orderCount":N ni topish (qattiq JSON parsing o'rniga)
        int idx = body.indexOf("\"orderCount\"");
        if (idx < 0) {
            return 0;
        }
        int colon = body.indexOf(':', idx);
        int comma = body.indexOf(',', colon);
        int end = comma < 0 ? body.indexOf('}', colon) : comma;
        if (colon < 0 || end < 0) {
            return 0;
        }
        try {
            return Long.parseLong(body.substring(colon + 1, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class UsageResponse {
        public UUID productId;
        public boolean used;
        public long orderCount;
    }
}
