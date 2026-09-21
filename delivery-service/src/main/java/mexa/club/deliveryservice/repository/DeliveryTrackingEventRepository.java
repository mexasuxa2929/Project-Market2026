package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.DeliveryTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingEventRepository extends JpaRepository<DeliveryTrackingEvent, UUID> {
    List<DeliveryTrackingEvent> findByDeliveryIdOrderByCreatedAtAsc(UUID deliveryId);
}
