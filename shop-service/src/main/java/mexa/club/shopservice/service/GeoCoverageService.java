package mexa.club.shopservice.service;

import mexa.club.shopservice.client.GeoApiClient;
import mexa.club.shopservice.dto.CoverageCheckResponse;
import mexa.club.shopservice.dto.CoverageZoneResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/** Mobil xarita uchun xizmat hududlari (geo-service'dan). */
@Service
public class GeoCoverageService {

    private final GeoApiClient geoApiClient;

    public GeoCoverageService(GeoApiClient geoApiClient) {
        this.geoApiClient = geoApiClient;
    }

    public List<CoverageZoneResponse> coverage() {
        return geoApiClient.fetchActiveZones().stream()
                .filter(z -> z.polygon() != null && !z.polygon().isBlank())
                .map(z -> new CoverageZoneResponse(z.id(), z.name(), z.polygon(), z.color()))
                .toList();
    }

    /** Nuqta xizmat hududidami — birinchi mos zona nomi bilan. */
    public CoverageCheckResponse check(double lat, double lng) {
        return geoApiClient.checkPoint(lat, lng).stream()
                .findFirst()
                .map(z -> new CoverageCheckResponse(true, z.name()))
                .orElse(new CoverageCheckResponse(false, null));
    }
}
