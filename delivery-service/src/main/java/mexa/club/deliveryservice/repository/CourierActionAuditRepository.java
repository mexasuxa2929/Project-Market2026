package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.CourierActionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourierActionAuditRepository extends JpaRepository<CourierActionAudit, UUID> {
    List<CourierActionAudit> findTop100ByOrderByCreatedAtDesc();
    List<CourierActionAudit> findTop100ByCourierIdOrderByCreatedAtDesc(UUID courierId);
    List<CourierActionAudit> findTop100ByActorIdOrderByCreatedAtDesc(UUID actorId);
}
