package mexa.club.shopservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** geo-service internal API mijozi (xizmat hududlari). */
@Component
public class GeoApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeoApiClient.class);

    private static final ParameterizedTypeReference<GeoEnvelope<List<GeoZoneDto>>> ACTIVE_ZONES_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<GeoEnvelope<List<GeoZoneDto>>> CHECK_POINT_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String internalSecret;
    private final String geoBaseUrl;

    public GeoApiClient(
            @Value("${GEO_SERVICE_URL:http://geo-service:8095}") String baseUrl,
            @Value("${APP_INTERNAL_SECRET:change-me-internal}") String internalSecret
    ) {
        String base = baseUrl == null ? "http://geo-service:8095" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.internalSecret = internalSecret;
        this.geoBaseUrl = base;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .requestFactory(requestFactory)
                .build();
    }

    /** Faol zonalar (bo'sh bo'lsa — bo'sh ro'yxat, xato yashiriladi). */
    public List<GeoZoneDto> fetchActiveZones() {
        try {
            log.info("Geo active zones: GET {}/internal/geo-zones/active", geoBaseUrl);
            List<GeoZoneDto> zones = getActiveZones(geoBaseUrl);
            log.info("Geo active zones: fetched {} zones", zones.size());
            return zones;
        } catch (Exception e) {
            // IDE'da GEO_SERVICE_URL ko'pincha docker nomi (geo-service) bo'lib qoladi —
            // u holda localhost fallback bilan qayta urinamiz.
            if (isHostUnreachable(e) && geoBaseUrl.contains("://geo-service")) {
                String fallback = geoBaseUrl.replace("://geo-service", "://localhost");
                try {
                    log.info("Geo active zones: retry via {}", fallback);
                    List<GeoZoneDto> zones = getActiveZones(fallback);
                    log.info("Geo active zones: fetched {} zones (fallback)", zones.size());
                    return zones;
                } catch (Exception retryEx) {
                    log.warn("Geo active zones fetch failed: {}", retryEx.toString());
                    return List.of();
                }
            }
            log.warn("Geo active zones fetch failed: {}", e.toString());
            return List.of();
        }
    }

    private List<GeoZoneDto> getActiveZones(String base) {
        GeoEnvelope<List<GeoZoneDto>> response = RestClient.builder()
                .baseUrl(base)
                .requestFactory(requestFactory())
                .build()
                .get()
                .uri("/internal/geo-zones/active")
                .header("X-Internal-Secret", internalSecret)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(ACTIVE_ZONES_TYPE);
        if (response != null && response.success() && response.data() != null) {
            return response.data();
        }
        log.warn("Geo active zones: empty/unexpected response");
        return List.of();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        return requestFactory;
    }

    /** Nuqta qaysi faol zonalarga kiradi (bo'sh bo'lsa — xizmat yo'q). */
    public List<GeoZoneDto> checkPoint(double lat, double lng) {
        try {
            GeoEnvelope<List<GeoZoneDto>> response = RestClient.builder()
                    .baseUrl(geoBaseUrl)
                    .requestFactory(requestFactory())
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/geo-zones/check-point")
                            .queryParam("lat", lat)
                            .queryParam("lng", lng)
                            .build())
                    .header("X-Internal-Secret", internalSecret)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(CHECK_POINT_TYPE);
            if (response != null && response.success() && response.data() != null) {
                return response.data();
            }
            return List.of();
        } catch (Exception e) {
            if (isHostUnreachable(e) && geoBaseUrl.contains("://geo-service")) {
                String fallback = geoBaseUrl.replace("://geo-service", "://localhost");
                try {
                    GeoEnvelope<List<GeoZoneDto>> response = RestClient.builder()
                            .baseUrl(fallback)
                            .requestFactory(requestFactory())
                            .build()
                            .get()
                            .uri(uriBuilder -> uriBuilder.path("/internal/geo-zones/check-point")
                                    .queryParam("lat", lat)
                                    .queryParam("lng", lng)
                                    .build())
                            .header("X-Internal-Secret", internalSecret)
                            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .body(CHECK_POINT_TYPE);
                    if (response != null && response.success() && response.data() != null) {
                        return response.data();
                    }
                    return List.of();
                } catch (Exception retryEx) {
                    log.warn("Geo check-point fetch failed: {}", retryEx.toString());
                    return List.of();
                }
            }
            log.warn("Geo check-point fetch failed: {}", e.toString());
            return List.of();
        }
    }

    private static boolean isHostUnreachable(Exception e) {        Throwable t = e;
        while (t != null) {
            if (t instanceof java.net.UnknownHostException
                    || t instanceof java.net.ConnectException
                    || t instanceof java.net.NoRouteToHostException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** geo-service envelope: {success, data, message}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoEnvelope<T>(boolean success, T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoZoneDto(
            UUID id,
            String name,
            String polygon,
            String color,
            boolean active
    ) {
    }
}
