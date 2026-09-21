package mexa.club.productservice.client;

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

@Component
public class WarehouseStockClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final String warehouseBaseUrl;
    private final String internalApiKey;

    public WarehouseStockClient(
            @Value("${app.warehouse-service.url:${WAREHOUSE_SERVICE_URL:http://localhost:8084}}") String warehouseBaseUrl,
            @Value("${app.internal-api.key:change-me-internal}") String internalApiKey
    ) {
        String base = warehouseBaseUrl == null ? "" : warehouseBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.warehouseBaseUrl = base;
        this.internalApiKey = internalApiKey;
    }

    public void deleteStockLinesByProductId(UUID productId) {
        if (productId == null) {
            return;
        }
        if (warehouseBaseUrl.isEmpty()) {
            throw new UpstreamReferenceException("Warehouse service base URL is not configured", null);
        }

        String url = warehouseBaseUrl + "/internal/stock-lines/variants/" + productId;
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
                throw new UpstreamReferenceException("Warehouse cleanup failed with status " + status, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Warehouse cleanup request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException("Warehouse cleanup service is unavailable", e);
        }
    }
}
