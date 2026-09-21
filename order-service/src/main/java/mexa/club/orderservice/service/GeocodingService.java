package mexa.club.orderservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

/**
 * Manzil matnini koordinataga aylantirish (Nominatim / OpenStreetMap).
 * Faqat geo-routing uchun: natija topilmasa empty — order baribir
 * yaratiladi (stock-bo'yicha eski allocate fallback).
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeocodingService(
            ObjectMapper objectMapper,
            @Value("${geocoding.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        String base = baseUrl != null ? baseUrl.trim() : "https://nominatim.openstreetmap.org";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Geocoding orderni kutdirmasligi uchun qisqa timeoutlar
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .requestFactory(requestFactory)
                .build();
    }

    public record LatLng(BigDecimal lat, BigDecimal lng) {}

    public record RegionDistrict(String region, String district) {}

    /**
     * Teskari geokodlash: nuqta → region/district (delivery yaratish uchun).
     * Topilmasa empty — delivery-service fallback narx ishlatadi.
     */
    public Optional<RegionDistrict> reverse(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", lat.toString())
                            .queryParam("lon", lng.toString())
                            .build())
                    .header("User-Agent", "MexaMarket-OrderService/1.0")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
            Object addrObj = root != null ? root.get("address") : null;
            if (!(addrObj instanceof Map)) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> addr = (Map<String, Object>) addrObj;
            String district = firstPresent(addr, "suburb", "city_district", "quarter", "neighbourhood", "county");
            String region = firstPresent(addr, "city", "town", "municipality", "state", "county");
            if ((district == null || district.isBlank()) && (region == null || region.isBlank())) {
                return Optional.empty();
            }
            return Optional.of(new RegionDistrict(
                    region != null ? region : "", district != null ? district : ""));
        } catch (Exception e) {
            log.warn("Reverse geocoding failed for {},{}: {}", lat, lng, e.getMessage());
            return Optional.empty();
        }
    }

    private static String firstPresent(Map<String, Object> addr, String... keys) {
        for (String k : keys) {
            Object v = addr.get(k);
            if (v != null && !v.toString().isBlank()) {
                return v.toString();
            }
        }
        return null;
    }

    /** "Ko'cha, tuman, shahar" matnidan birinchi topilgan nuqta. */
    public Optional<LatLng> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .queryParam("countrycodes", "uz")
                            .queryParam("q", query.trim())
                            .build())
                    .header("User-Agent", "MexaMarket-OrderService/1.0")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            List<Map<String, Object>> list = objectMapper.readValue(body, new TypeReference<>() {});
            if (list == null || list.isEmpty()) {
                return Optional.empty();
            }
            Object lat = list.get(0).get("lat");
            Object lng = list.get(0).get("lon");
            if (lat == null || lng == null) {
                return Optional.empty();
            }
            return Optional.of(new LatLng(new BigDecimal(lat.toString()), new BigDecimal(lng.toString())));
        } catch (Exception e) {
            log.warn("Geocoding failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }
}
