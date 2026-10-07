package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {
    List<PricingRule> findAllByOrderByPriorityDescCreatedAtDesc();
}
