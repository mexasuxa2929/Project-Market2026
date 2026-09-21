package mexa.club.shopservice.repository;

import mexa.club.shopservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCart_IdOrderByCreatedAtAsc(UUID cartId);

    Optional<CartItem> findByCart_IdAndProductId(UUID cartId, UUID productId);

    void deleteByCart_Id(UUID cartId);

    void deleteByCart_IdAndProductId(UUID cartId, UUID productId);

    long countByCart_Id(UUID cartId);
}
