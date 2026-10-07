package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.DeliverySurcharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliverySurchargeRepository extends JpaRepository<DeliverySurcharge, UUID> {
    List<DeliverySurcharge> findAllByOrderByPriorityDescCreatedAtAsc();
}
