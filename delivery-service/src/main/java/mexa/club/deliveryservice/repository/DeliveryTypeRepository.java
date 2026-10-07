package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.DeliveryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryTypeRepository extends JpaRepository<DeliveryType, UUID> {
    Optional<DeliveryType> findByNameIgnoreCase(String name);
}
