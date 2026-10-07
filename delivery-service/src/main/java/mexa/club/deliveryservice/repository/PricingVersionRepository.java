package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.PricingVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingVersionRepository extends JpaRepository<PricingVersion, UUID> {
    List<PricingVersion> findAllByOrderByVersionDesc();
    PricingVersion findTopByOrderByVersionDesc();
}
