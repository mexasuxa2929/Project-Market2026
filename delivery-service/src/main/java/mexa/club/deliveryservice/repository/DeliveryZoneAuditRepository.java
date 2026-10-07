package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.DeliveryZoneAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryZoneAuditRepository extends JpaRepository<DeliveryZoneAudit, UUID> {
    List<DeliveryZoneAudit> findTop50ByOrderByCreatedAtDesc();
    List<DeliveryZoneAudit> findTop50ByZoneIdOrderByCreatedAtDesc(UUID zoneId);
}
