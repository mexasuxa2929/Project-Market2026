package mexa.club.productservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.productservice.exception.UpstreamReferenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ShopReviewClient {

    private static final Logger log = LoggerFactory.getLogger(ShopReviewClient.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String shopBaseUrl;
    private final String internalApiKey;

    public ShopReviewClient(
            @Value("${app.shop-service.url:${SHOP_SERVICE_URL:http://localhost:8094}}") String shopBaseUrl,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        String base = shopBaseUrl == null ? "" : shopBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.shopBaseUrl = base;
        this.internalApiKey = internalApiKey;
    }

    public void deleteReviewsByProductId(UUID productId) {
        if (productId == null) {
            return;
        }
        if (shopBaseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Shop service base URL is not configured", null);
        }

        String url = shopBaseUrl + "/api/shops/internal/products/" + productId + "/reviews";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-Internal-Api-Key", internalApiKey)
                .DELETE()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new UpstreamReferenceException("Review cleanup failed with status " + status, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Review cleanup request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Review cleanup service is unavailable", e);
        }
    }

    /** Bir nechta mahsulot uchun reytingni batch qilib olish. Qaytaradi: productId → [avgRating, reviewCount] */
    public Map<UUID, double[]> fetchRatingBatch(java.util.List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty() || shopBaseUrl.isEmpty()) {
            return Map.of();
        }
        String idsParam = productIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        String url = shopBaseUrl + "/api/shops/products/rating-batch?ids=" + idsParam;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-Internal-Api-Key", internalApiKey)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.warn("Rating batch fetch failed with status {}", status);
                return Map.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            Map<UUID, double[]> result = new java.util.HashMap<>();
            data.fields().forEachRemaining(entry -> {
                try {
                    UUID id = UUID.fromString(entry.getKey());
                    JsonNode arr = entry.getValue();
                    double avg = arr.get(0).asDouble();
                    double cnt = arr.get(1).asDouble();
                    result.put(id, new double[]{avg, cnt});
                } catch (Exception e) {
                    log.warn("Failed to parse rating for {}: {}", entry.getKey(), e.getMessage());
                }
            });
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rating batch request interrupted");
            return Map.of();
        } catch (IOException e) {
            log.warn("Rating batch service unavailable: {}", e.getMessage());
            return Map.of();
        }
    }
}
