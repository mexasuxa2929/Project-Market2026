package mexa.club.geoservice.service;

import mexa.club.geoservice.dto.GeoZoneRequest;
import mexa.club.geoservice.dto.GeoZoneResponse;
import mexa.club.geoservice.dto.PointInPolygonRequest;
import mexa.club.geoservice.entity.GeoZone;
import mexa.club.geoservice.exception.GeoException;
import mexa.club.geoservice.repository.GeoZoneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GeoZoneService {
    private final GeoZoneRepository repository;
    private final GeoZoneMapper mapper;

    public GeoZoneService(GeoZoneRepository repository, GeoZoneMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GeoZoneResponse> all() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GeoZoneResponse> allByWarehouse(UUID warehouseId) {
        return repository.findByWarehouseId(warehouseId).stream().map(mapper::toResponse).toList();
    }

    /** Mobil ilova uchun: faqat faol xizmat hududlari (xaritada chiziladi). */
    @Transactional(readOnly = true)
    public List<GeoZoneResponse> allActive() {
        return repository.findByActiveTrue().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GeoZoneResponse getById(UUID id) {
        return mapper.toResponse(getZone(id));
    }

    @Transactional
    public GeoZoneResponse create(GeoZoneRequest request) {
        GeoZone z = new GeoZone();
        applyRequest(z, request);
        return mapper.toResponse(repository.save(z));
    }

    @Transactional
    public GeoZoneResponse update(UUID id, GeoZoneRequest request) {
        GeoZone z = getZone(id);
        applyRequest(z, request);
        return mapper.toResponse(repository.save(z));
    }

    @Transactional
    public void delete(UUID id) {
        getZone(id);
        repository.deleteById(id);
    }

    @Transactional
    public void deleteByWarehouseId(UUID warehouseId) {
        repository.deleteByWarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<GeoZoneResponse> checkPoint(PointInPolygonRequest request) {
        double lat = Double.parseDouble(request.lat());
        double lng = Double.parseDouble(request.lng());
        return repository.findByActiveTrue().stream()
                .filter(z -> z.getPolygon() != null && !z.getPolygon().isBlank())
                .filter(z -> pointInPolygon(lat, lng, parsePolygon(z.getPolygon())))
                .map(mapper::toResponse)
                .toList();
    }

    private GeoZone getZone(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new GeoException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Geo zone not found"));
    }

    private void applyRequest(GeoZone z, GeoZoneRequest r) {
        z.setName(r.name());
        z.setRegion(r.region());
        z.setDistrict(r.district());
        z.setPolygon(r.polygon());
        z.setCenterLat(r.centerLat());
        z.setCenterLng(r.centerLng());
        z.setZoomLevel(r.zoomLevel() != null ? r.zoomLevel() : 12);
        z.setFee(r.fee() != null ? r.fee() : java.math.BigDecimal.ZERO);
        z.setEstimatedDays(r.estimatedDays() != null ? r.estimatedDays() : 1);
        z.setColor(r.color() != null ? r.color() : "#6366F1");
        z.setActive(r.active());
        if (r.warehouseId() != null && !r.warehouseId().isBlank()) {
            z.setWarehouseId(UUID.fromString(r.warehouseId()));
        } else {
            z.setWarehouseId(null);
        }
    }

    private List<double[]> parsePolygon(String polygon) {
        polygon = polygon.trim();
        if (polygon.startsWith("[") && polygon.endsWith("]")) {
            polygon = polygon.substring(1, polygon.length() - 1);
        }
        String[] parts = polygon.split("\\],\\s*\\[");
        return java.util.Arrays.stream(parts).map(p -> {
            p = p.replaceAll("[\\[\\]]", "").trim();
            String[] coords = p.split(",");
            if (coords.length < 2) return null;
            return new double[]{Double.parseDouble(coords[0].trim()), Double.parseDouble(coords[1].trim())};
        }).filter(java.util.Objects::nonNull).toList();
    }

    private boolean pointInPolygon(double lat, double lng, List<double[]> polygon) {
        int n = polygon.size();
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double[] vi = polygon.get(i);
            double[] vj = polygon.get(j);
            if ((vi[0] > lat) != (vj[0] > lat)
                    && lng < (vj[1] - vi[1]) * (lat - vi[0]) / (vj[0] - vi[0]) + vi[1]) {
                inside = !inside;
            }
        }
        return inside;
    }
}
