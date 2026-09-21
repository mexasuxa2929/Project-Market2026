package mexa.club.productservice.repository;

import mexa.club.productservice.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    List<ProductPrice> findByProductIdOrderByEffectiveDateDesc(UUID productId);

    /** BATCH: bir nechta mahsulot uchun barcha narxlar (effectiveDate DESC) — N+1 o'rniga bitta query. */
    List<ProductPrice> findByProductIdInOrderByEffectiveDateDesc(Collection<UUID> productIds);

    Optional<ProductPrice> findByIdAndProductId(UUID id, UUID productId);

    void deleteByProductId(UUID productId);
}

