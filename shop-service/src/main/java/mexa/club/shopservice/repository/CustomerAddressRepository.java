package mexa.club.shopservice.repository;

import mexa.club.shopservice.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findAllByUserIdOrderByDefaultAddressDescCreatedAtAsc(UUID userId);

    Optional<CustomerAddress> findByIdAndUserId(UUID id, UUID userId);

    List<CustomerAddress> findAllByUserIdAndDefaultAddressTrue(UUID userId);
}
