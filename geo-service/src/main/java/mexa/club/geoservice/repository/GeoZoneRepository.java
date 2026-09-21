package mexa.club.geoservice.repository;

import mexa.club.geoservice.entity.GeoZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeoZoneRepository extends JpaRepository<GeoZone, UUID> {
    List<GeoZone> findByActiveTrue();
    List<GeoZone> findByRegionIgnoreCase(String region);
    List<GeoZone> findByWarehouseId(UUID warehouseId);
    void deleteByWarehouseId(UUID warehouseId);
}
