package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.PricingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PricingAuditLogRepository extends JpaRepository<PricingAuditLog, UUID> {
}
