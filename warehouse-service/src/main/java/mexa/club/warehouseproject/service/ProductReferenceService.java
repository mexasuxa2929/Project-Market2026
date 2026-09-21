package mexa.club.warehouseproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import mexa.club.warehouseproject.exception.UpstreamReferenceException;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductReferenceService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final ObjectMapper objectMapper;
    private final String productServiceBaseUrl;

    public ProductReferenceService(
            ObjectMapper objectMapper,
            @Value("${services.product.url:${PRODUCT_SERVICE_URL:http://localhost:8083}}") String productServiceBaseUrl
    ) {
        this.objectMapper = objectMapper;
        String base = productServiceBaseUrl == null ? "" : productServiceBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.productServiceBaseUrl = base;
    }

    // ── Product-level lookup (used for minStock in low-stock alerts, and for
    //    validating productId references after the variant system was removed —
    //    each color is now its own independent Product row) ───────────────────

    public boolean existsProduct(UUID productId) {
        return fetchProduct(productId) != null;
    }

    public Optional<ProductSnapshot> findProduct(UUID productId) {
        return Optional.ofNullable(fetchProduct(productId));
    }

    public Map<UUID, ProductSnapshot> fetchProducts(Set<UUID> productIds) {
        Map<UUID, ProductSnapshot> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        for (UUID id : productIds) {
            try {
                ProductSnapshot snapshot = fetchProduct(id);
                if (snapshot != null) {
                    result.put(id, snapshot);
                }
            } catch (Exception e) {
                // skip unavailable — caller uses "Unknown"
            }
        }
        return result;
    }

    /**
     * Backward-compatible alias used by stock/movement/report services that previously
     * resolved "variant" display names. Since variants were removed, this now resolves
     * the product itself and exposes it via a VariantSnapshot-shaped record for minimal
     * call-site churn.
     */
    public Optional<VariantSnapshot> findVariant(UUID productId) {
        return findProduct(productId).map(p -> new VariantSnapshot(p.id(), p.name(), null));
    }

    public Map<UUID, VariantSnapshot> fetchVariants(Set<UUID> productIds) {
        Map<UUID, VariantSnapshot> result = new HashMap<>();
        fetchProducts(productIds).forEach((id, p) -> result.put(id, new VariantSnapshot(p.id(), p.name(), null)));
        return result;
    }

    public void requireProductExists(UUID productId) {
        if (!existsProduct(productId)) {
            throw new ResourceNotFoundException("Product", String.valueOf(productId));
        }
    }

    // ── Legacy alias kept so call sites that previously validated "variant"
    //    existence now validate the product itself. ────────────────────────────

    public void requireExists(UUID productId) {
        if (!existsProduct(productId)) {
            throw new ResourceNotFoundException("Product", String.valueOf(productId));
        }
    }

    /**
     * Returns the current base sale price (latest {@code product_price.salePrice})
     * from product-service's {@code GET /api/products/{id}/full}. Empty when the
     * product does not exist or has no price yet.
     */
    public Optional<BigDecimal> findSalePrice(UUID productId) {
        if (productId == null || productServiceBaseUrl.isEmpty()) {
            return Optional.empty();
        }
        String url = productServiceBaseUrl + "/api/products/" + productId + "/full";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET();
        String auth = currentAuthorizationHeader();
        if (auth != null && !auth.isBlank()) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, auth);
        }
        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) return Optional.empty();
            if (status < 200 || status >= 300) {
                throw new UpstreamReferenceException("Product service returned status " + status);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            JsonNode basePrice = data.path("basePrice");
            if (!basePrice.isNumber()) {
                return Optional.empty();
            }
            return Optional.of(basePrice.decimalValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Product service request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Product service is unavailable", e);
        }
    }

    /**
     * Resolves product IDs whose name matches the given partial search text by
     * querying product-service {@code GET /api/products?name=...}. Used to filter
     * warehouse stock by product name (product names live in product-service).
     */
    public List<UUID> searchProductIds(String name) {
        if (name == null || name.isBlank() || productServiceBaseUrl.isEmpty()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        String url = productServiceBaseUrl + "/api/products?name=" + encoded + "&size=500&sort=name";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET();
        String auth = currentAuthorizationHeader();
        if (auth != null && !auth.isBlank()) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, auth);
        }
        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new UpstreamReferenceException("Product service returned status " + status);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("data").path("content");
            if (!content.isArray()) {
                return List.of();
            }
            List<UUID> ids = new ArrayList<>();
            for (JsonNode node : content) {
                JsonNode idNode = node.get("id");
                if (idNode != null && idNode.isTextual()) {
                    try {
                        ids.add(UUID.fromString(idNode.asText()));
                    } catch (IllegalArgumentException ignored) {
                        // skip non-UUID ids
                    }
                }
            }
            return ids;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Product service request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Product service is unavailable", e);
        }
    }

    private ProductSnapshot fetchProduct(UUID productId) {
        if (productId == null) return null;
        if (productServiceBaseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Product service base URL is not configured");
        }
        String url = productServiceBaseUrl + "/api/products/" + productId;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET();
        String auth = currentAuthorizationHeader();
        if (auth != null && !auth.isBlank()) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, auth);
        }
        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) return null;
            if (status == 401 || status == 403) {
                throw new UpstreamReferenceException("Product service authorization failed with status " + status);
            }
            if (status < 200 || status >= 300) {
                throw new UpstreamReferenceException("Product service returned status " + status);
            }
            return parseProductResponse(productId, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Product service request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Product service is unavailable", e);
        }
    }

    private ProductSnapshot parseProductResponse(UUID requestedId, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) return null;
        UUID id = requestedId;
        JsonNode idNode = data.get("id");
        if (idNode != null && !idNode.isNull() && idNode.isTextual()) {
            try { id = UUID.fromString(idNode.asText()); } catch (IllegalArgumentException ignored) {}
        }
        String name = data.path("name").isTextual() ? data.path("name").asText() : null;
        int minStock = data.path("minStock").isNumber() ? data.path("minStock").asInt() : 0;
        double length = data.path("length").isNumber() ? data.path("length").asDouble() : 0.0;
        double width = data.path("width").isNumber() ? data.path("width").asDouble() : 0.0;
        double height = data.path("height").isNumber() ? data.path("height").asDouble() : 0.0;
        return new ProductSnapshot(id, name, minStock, length, width, height);
    }

    private static String currentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request != null ? request.getHeader(HttpHeaders.AUTHORIZATION) : null;
    }

    public record ProductSnapshot(UUID id, String name, int minStock, double length, double width, double height) {}

    /**
     * Kept for call-site compatibility after variant removal — "sku" is now always null
     * since products no longer have a separate variant SKU; barcode/name come from the
     * product itself.
     */
    public record VariantSnapshot(UUID id, String name, String sku) {}
}
