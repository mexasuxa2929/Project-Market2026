package mexa.club.productservice.repository;

import mexa.club.productservice.entity.ProductPriceTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductPriceTierRepository extends JpaRepository<ProductPriceTier, UUID> {

    List<ProductPriceTier> findByProductIdOrderByMinQtyAsc(UUID productId);

    /** BATCH: ko'p mahsulot liniyalarini bitta so'rovda olish (N+1 o'rniga). */
    @Query("SELECT t FROM ProductPriceTier t WHERE t.productId IN :productIds ORDER BY t.minQty ASC")
    List<ProductPriceTier> findByProductIdInOrderByMinQtyAsc(@Param("productIds") Collection<UUID> productIds);

    void deleteByProductId(UUID productId);

    /** Berilgan miqdor uchun mos narx bosqichini topadi */
    @Query("""
        SELECT t FROM ProductPriceTier t
        WHERE t.productId = :productId
          AND t.minQty <= :qty
          AND (t.maxQty IS NULL OR t.maxQty >= :qty)
        ORDER BY t.minQty DESC
        """)
    Optional<ProductPriceTier> findMatchingTier(
            @Param("productId") UUID productId,
            @Param("qty") int qty
    );
}
