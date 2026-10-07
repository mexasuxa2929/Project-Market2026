package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.FreeDeliveryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FreeDeliveryRuleRepository extends JpaRepository<FreeDeliveryRule, UUID> {
    List<FreeDeliveryRule> findAllByOrderByMinOrderAsc();
}
