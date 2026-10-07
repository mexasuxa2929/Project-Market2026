package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.PricingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PricingSettingsRepository extends JpaRepository<PricingSettings, UUID> {
}
