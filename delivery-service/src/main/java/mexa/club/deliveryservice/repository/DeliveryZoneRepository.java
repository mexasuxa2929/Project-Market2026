package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {
    Optional<DeliveryZone> findByRegionIgnoreCaseAndDistrictIgnoreCase(String region, String district);
}
