package mexa.club.shopservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.shopservice.dto.GeoSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Manzil qidiruv: user matn kiritadi → nomzodlar ro'yxati (name + lat + lng).
 * User bittasini tanlaydi — viloyat/tuman matn kiritish yo'q.
 */
@Service
public class GeoSearchService {

    private static final Logger log = LoggerFactory.getLogger(GeoSearchService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeoSearchService(
            ObjectMapper objectMapper,
            @Value("${geocoding.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        String base = baseUrl != null ? baseUrl.trim() : "https://nominatim.openstreetmap.org";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .requestFactory(requestFactory)
                .build();
    }

    public List<GeoSearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "uz")
                            .queryParam("q", query.trim())
                            .build())
                    .header("User-Agent", "MexaMarket-ShopService/1.0")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            List<Map<String, Object>> list = objectMapper.readValue(body, new TypeReference<>() {});
            if (list == null) {
                return List.of();
            }
            return list.stream()
                    .filter(m -> m.get("lat") != null && m.get("lon") != null)
                    .map(m -> new GeoSearchResult(
                            m.get("display_name") != null ? String.valueOf(m.get("display_name")) : query.trim(),
                            new BigDecimal(String.valueOf(m.get("lat"))),
                            new BigDecimal(String.valueOf(m.get("lon")))))
                    .toList();
        } catch (Exception e) {
            log.warn("Geo search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public record ReverseResult(String name, BigDecimal lat, BigDecimal lng) {}

    /**
     * Teskari geokodlash: nuqta → manzil nomi (xaritadan tanlanganda).
     */
    public ReverseResult reverse(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return null;
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", lat.toString())
                            .queryParam("lon", lng.toString())
                            .build())
                    .header("User-Agent", "MexaMarket-ShopService/1.0")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
            if (root == null || root.get("display_name") == null) {
                return null;
            }
            return new ReverseResult(String.valueOf(root.get("display_name")), lat, lng);
        } catch (Exception e) {
            log.warn("Geo reverse failed for {},{}: {}", lat, lng, e.getMessage());
            return null;
        }
    }
}
