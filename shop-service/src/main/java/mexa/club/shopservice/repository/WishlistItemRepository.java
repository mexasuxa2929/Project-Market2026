package mexa.club.shopservice.repository;

import mexa.club.shopservice.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<WishlistItem> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<WishlistItem> findByUserIdAndProductId(UUID userId, UUID productId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
