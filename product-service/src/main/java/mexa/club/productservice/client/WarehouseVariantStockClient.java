package mexa.club.productservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calls warehouse-service POST /api/stock/batch to get aggregated stock per product.
 * Renamed from variant-keyed to product-keyed after variants were removed: every "rang"
 * (color) is now its own Product, so stock is tracked directly against productId.
 */
@Component
public class WarehouseVariantStockClient {

    private static final Logger log = LoggerFactory.getLogger(WarehouseVariantStockClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final ObjectMapper objectMapper;
    private final String warehouseBaseUrl;

    public WarehouseVariantStockClient(
            ObjectMapper objectMapper,
            @Value("${services.warehouse.url:${WAREHOUSE_SERVICE_URL:http://localhost:8084}}") String warehouseBaseUrl
    ) {
        this.objectMapper = objectMapper;
        String base = warehouseBaseUrl == null ? "" : warehouseBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.warehouseBaseUrl = base;
    }

    /**
     * Returns a map of productId → totalStock (sum across all warehouses).
     * If warehouse-service is unavailable or returns an error, returns an empty map so the caller
     * can still respond with stock=null and inStock=false.
     */
    public Map<UUID, Integer> fetchTotalStockBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("productIds", productIds.stream().map(UUID::toString).collect(Collectors.toList()))
            );
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(warehouseBaseUrl + "/api/stock/batch"))
                    .timeout(Duration.ofSeconds(5))
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            String auth = currentAuthorizationHeader();
            if (auth != null && !auth.isBlank()) {
                reqBuilder.header(HttpHeaders.AUTHORIZATION, auth);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Warehouse batch stock returned status {}", response.statusCode());
                return Collections.emptyMap();
            }

            return parseBatchResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Warehouse batch stock request interrupted");
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Warehouse batch stock unavailable: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Convenience for a single product. */
    public int fetchTotalStock(UUID productId) {
        if (productId == null) {
            return 0;
        }
        return fetchTotalStockBatch(List.of(productId)).getOrDefault(productId, 0);
    }

    private Map<UUID, Integer> parseBatchResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        // Response can be direct array or wrapped in {data: [...]}
        JsonNode arrayNode = root.isArray() ? root : root.path("data");
        if (!arrayNode.isArray()) {
            return Collections.emptyMap();
        }
        Map<UUID, Integer> result = new java.util.LinkedHashMap<>();
        for (JsonNode item : arrayNode) {
            String pidStr = item.path("productId").asText(null);
            if (pidStr == null || pidStr.isBlank()) continue;
            try {
                UUID productId = UUID.fromString(pidStr);
                int total = item.path("totalStock").asInt(0);
                result.put(productId, total);
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }
        return result;
    }

    private static String currentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request != null ? request.getHeader(HttpHeaders.AUTHORIZATION) : null;
    }
}
