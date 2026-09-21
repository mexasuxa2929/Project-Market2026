package mexa.club.warehouseproject.repository;

import jakarta.persistence.LockModeType;
import mexa.club.warehouseproject.entity.StockReturn;
import mexa.club.warehouseproject.entity.StockReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReturnRepository extends JpaRepository<StockReturn, UUID> {
    Page<StockReturn> findByWarehouseIdOrderByCreatedAtDesc(UUID warehouseId, Pageable pageable);
    Optional<StockReturn> findByIdAndWarehouseId(UUID id, UUID warehouseId);

    List<StockReturn> findByWarehouseIdAndOrderIdAndStatus(UUID warehouseId, UUID orderId, StockReturnStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReturn r WHERE r.id = :id AND r.warehouseId = :warehouseId")
    Optional<StockReturn> findByIdAndWarehouseIdForUpdate(
            @Param("id") UUID id,
            @Param("warehouseId") UUID warehouseId
    );

    @Modifying
    @Query("DELETE FROM StockReturn r WHERE r.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") UUID warehouseId);
}
