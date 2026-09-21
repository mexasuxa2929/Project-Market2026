package mexa.club.geoservice.repository;

import mexa.club.geoservice.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, UUID> {
}
