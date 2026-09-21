package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    @Query("""
            SELECT p FROM Purchase p
            WHERE p.warehouse.id = :warehouseId
            ORDER BY p.purchaseDate DESC, p.id DESC
            """)
    Page<Purchase> findByWarehouseId(@Param("warehouseId") UUID warehouseId, Pageable pageable);

    @Query("""
            SELECT p FROM Purchase p
            WHERE p.id = :id AND p.warehouse.id = :warehouseId
            """)
    Optional<Purchase> findByIdAndWarehouseId(@Param("id") UUID id, @Param("warehouseId") UUID warehouseId);

    boolean existsByWarehouse_Id(UUID warehouseId);

    long deleteByWarehouse_Id(UUID warehouseId);
}
