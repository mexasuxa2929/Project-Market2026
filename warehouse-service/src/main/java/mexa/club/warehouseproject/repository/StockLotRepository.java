package mexa.club.warehouseproject.repository;

import jakarta.persistence.LockModeType;
import mexa.club.warehouseproject.entity.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLotRepository extends JpaRepository<StockLot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM StockLot l
            WHERE l.warehouseId = :warehouseId AND l.productId = :productId
            ORDER BY l.receivedDate ASC, l.id ASC
            """)
    List<StockLot> findLotsForUpdate(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId);

    @Query("""
            SELECT COALESCE(SUM(l.quantity), 0) FROM StockLot l
            WHERE l.warehouseId = :warehouseId AND l.productId = :productId
            """)
    BigDecimal sumQuantity(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId);

    List<StockLot> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    /** Avto-zakup narxi uchun oxirgi kirim tannarxi */
    Optional<StockLot> findTopByWarehouseIdAndProductIdOrderByReceivedDateDesc(UUID warehouseId, UUID productId);

    List<StockLot> findByWarehouseIdOrderByReceivedDateAsc(UUID warehouseId);

    List<StockLot> findByPurchaseId(UUID purchaseId);

    List<StockLot> findByPurchaseItemId(UUID purchaseItemId);

    @Modifying
    @Query("DELETE FROM StockLot l WHERE l.warehouseId = :warehouseId AND l.productId = :productId")
    void deleteByWarehouseIdAndProductId(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM StockLot l WHERE l.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") UUID warehouseId);

    @Modifying
    @Query("DELETE FROM StockLot l WHERE l.purchaseId = :purchaseId")
    void deleteByPurchaseId(@Param("purchaseId") UUID purchaseId);

    @Modifying
    @Query("DELETE FROM StockLot l WHERE l.productId = :productId")
    void deleteAllByProductId(@Param("productId") UUID productId);
}
