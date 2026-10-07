package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.CartIncentiveRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartIncentiveRuleRepository extends JpaRepository<CartIncentiveRule, UUID> {
    List<CartIncentiveRule> findAllByOrderByThresholdAmountAsc();
}
