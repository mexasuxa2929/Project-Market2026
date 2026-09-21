package mexa.club.orderservice.client;

import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * geo-service polygon tekshiruvi: nuqta qaysi zona ichida ekanini
 * aniqlaydi. /internal/** permitAll — auth shart emas.
 */
@FeignClient(name = "geoZoneClient", url = "${services.geo.url:http://localhost:8095}", configuration = FeignConfig.class)
public interface GeoZoneClient {

    @GetMapping("/internal/geo-zones/check-point")
    GeoZoneEnvelope checkPoint(
            @RequestParam("lat") String lat,
            @RequestParam("lng") String lng
    );

    record GeoZoneEnvelope(boolean success, List<GeoZonePayload> data, String message) {}

    record GeoZonePayload(
            UUID id,
            String name,
            String region,
            String district,
            UUID warehouseId
    ) {}
}
