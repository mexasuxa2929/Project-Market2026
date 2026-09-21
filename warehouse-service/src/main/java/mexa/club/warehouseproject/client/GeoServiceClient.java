package mexa.club.warehouseproject.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class GeoServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public GeoServiceClient(
            @Qualifier("geoServiceRestTemplate") RestTemplate restTemplate,
            @Value("${services.geo.url:${GEO_SERVICE_URL:http://localhost:8095}}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.baseUrl = base;
    }

    public void deleteZoneDataByWarehouseId(UUID warehouseId) {
        if (warehouseId == null || baseUrl.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = currentAuthorizationHeader();
            if (auth != null && !auth.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, auth);
            }
            restTemplate.exchange(
                    baseUrl + "/internal/geo-zones/by-warehouse/" + warehouseId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (Exception ignored) {
            // geo-service mavjud bo'lmasa yoki xatolik bo'lsa — warehouse o'chirilishini to'xtatmaymiz
        }
    }

    private static String currentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request != null ? request.getHeader(HttpHeaders.AUTHORIZATION) : null;
    }
}
