package mexa.club.warehouseproject.repository;

import jakarta.persistence.LockModeType;
import mexa.club.warehouseproject.entity.WarehouseStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, UUID> {

    @Query("""
            SELECT s FROM WarehouseStock s
            WHERE s.warehouse.id = :warehouseId
            ORDER BY s.productId ASC
            """)
    Page<WarehouseStock> findByWarehouseId(@Param("warehouseId") UUID warehouseId, Pageable pageable);

    @Query("""
            SELECT s FROM WarehouseStock s
            WHERE s.warehouse.id = :warehouseId AND s.productId IN :productIds
            ORDER BY s.productId ASC
            """)
    Page<WarehouseStock> findByWarehouseIdAndProductIdIn(
            @Param("warehouseId") UUID warehouseId,
            @Param("productIds") List<UUID> productIds,
            Pageable pageable
    );

    Optional<WarehouseStock> findByWarehouse_IdAndProductId(UUID warehouseId, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM WarehouseStock s WHERE s.warehouse.id = :warehouseId AND s.productId = :productId")
    Optional<WarehouseStock> findByWarehouseIdAndProductIdForUpdate(
            @Param("warehouseId") UUID warehouseId,
            @Param("productId") UUID productId
    );

    List<WarehouseStock> findAllByWarehouse_Id(UUID warehouseId);

    @Query("SELECT s FROM WarehouseStock s WHERE s.warehouse.id IN :warehouseIds")
    List<WarehouseStock> findAllByWarehouseIdIn(@Param("warehouseIds") Collection<UUID> warehouseIds);

    @Query("SELECT COUNT(s) FROM WarehouseStock s WHERE s.warehouse.id = :warehouseId")
    long countByWarehouseId(@Param("warehouseId") UUID warehouseId);

    @Query("""
            SELECT s FROM WarehouseStock s
            JOIN FETCH s.warehouse w
            ORDER BY w.id ASC, s.productId ASC
            """)
    List<WarehouseStock> findAllWithWarehouse();

    boolean existsByWarehouse_Id(UUID warehouseId);

    long deleteAllByProductId(UUID productId);

    long deleteByWarehouse_Id(UUID warehouseId);

    @Query("""
            SELECT s FROM WarehouseStock s
            WHERE s.productId IN :productIds
            """)
    List<WarehouseStock> findAllByProductIdIn(@Param("productIds") List<UUID> productIds);
}
