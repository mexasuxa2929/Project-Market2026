package mexa.club.shopservice.repository;

import mexa.club.shopservice.entity.ShopCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopCartRepository extends JpaRepository<ShopCart, UUID> {

    Optional<ShopCart> findByUserId(UUID userId);
}
