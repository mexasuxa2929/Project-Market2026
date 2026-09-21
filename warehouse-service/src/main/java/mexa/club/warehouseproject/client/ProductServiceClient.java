package mexa.club.warehouseproject.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import mexa.club.warehouseproject.exception.UpstreamReferenceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * product-service orqali brand, category, manufacturer katalogini oqish.
 * Ombor o'zida jadval yaratilmaydi — faqat UUID saqlanadi, nomlar kerak bo'lganda shu clientdan olinadi.
 */
@Component
public class ProductServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ProductServiceClient(
            @Qualifier("productServiceRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${services.product.url:${PRODUCT_SERVICE_URL:http://localhost:8083}}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.baseUrl = base;
    }

    /**
     * Brand nomini qaytaradi yoki product-service mavjud emas bo'lsa empty.
     */
    public Optional<ProductCatalogItem> getBrand(UUID id) {
        return fetchCatalogItem("/api/brands/" + id, id);
    }

    public Optional<ProductCatalogItem> getCategory(UUID id) {
        return fetchCatalogItem("/api/categories/" + id, id);
    }

    public Optional<ProductCatalogItem> getManufacturer(UUID id) {
        return fetchCatalogItem("/api/manufacturers/" + id, id);
    }

    public Optional<String> resolveBrandName(UUID id) {
        return getBrand(id).map(ProductCatalogItem::name);
    }

    public Optional<String> resolveCategoryName(UUID id) {
        return getCategory(id).map(ProductCatalogItem::name);
    }

    public Optional<String> resolveManufacturerName(UUID id) {
        return getManufacturer(id).map(ProductCatalogItem::name);
    }

    private Optional<ProductCatalogItem> fetchCatalogItem(String path, UUID requestedId) {
        if (requestedId == null) {
            return Optional.empty();
        }
        if (baseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Product service base URL is not configured (services.product.url)");
        }
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        String auth = currentAuthorizationHeader();
        if (auth != null && !auth.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, auth);
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(parseItem(requestedId, response.getBody()));
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                return Optional.empty();
            }
            if (status == 401 || status == 403) {
                throw new UpstreamReferenceException("Product service authorization failed with status " + status);
            }
            if (status >= 500) {
                throw new UpstreamReferenceException("Product service returned status " + status);
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new UpstreamReferenceException("Product service response could not be read", e);
        } catch (Exception e) {
            throw new UpstreamReferenceException("Product service is unavailable", e);
        }
    }

    private ProductCatalogItem parseItem(UUID requestedId, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }
        UUID id = requestedId;
        JsonNode idNode = data.get("id");
        if (idNode != null && !idNode.isNull() && idNode.isTextual()) {
            try {
                id = UUID.fromString(idNode.asText());
            } catch (IllegalArgumentException ignored) {
                id = requestedId;
            }
        }
        String name = textOrNull(data, "name");
        String description = textOrNull(data, "description");
        String address = textOrNull(data, "address");
        String contactInfo = textOrNull(data, "contactInfo");
        return new ProductCatalogItem(id, name, description, address, contactInfo);
    }

    private static String textOrNull(JsonNode data, String field) {
        JsonNode n = data.get(field);
        if (n == null || n.isNull() || !n.isTextual()) {
            return null;
        }
        String t = n.asText();
        return t != null && t.isBlank() ? null : t;
    }

    private static String currentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request != null ? request.getHeader(HttpHeaders.AUTHORIZATION) : null;
    }

    public record ProductCatalogItem(
            UUID id,
            String name,
            String description,
            String address,
            String contactInfo
    ) {}
}
