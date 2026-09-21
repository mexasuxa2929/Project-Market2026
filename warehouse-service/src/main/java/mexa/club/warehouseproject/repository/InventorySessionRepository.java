package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.InventorySession;
import mexa.club.warehouseproject.entity.InventoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventorySessionRepository extends JpaRepository<InventorySession, UUID> {
    @EntityGraph(attributePaths = {"items"})
    Page<InventorySession> findByWarehouseIdOrderByStartedAtDesc(UUID warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Optional<InventorySession> findByIdAndWarehouseId(UUID sessionId, UUID warehouseId);
    boolean existsByWarehouseIdAndStatusIn(UUID warehouseId, List<InventoryStatus> statuses);

    @Query("SELECT COUNT(s) FROM InventorySession s WHERE s.warehouseId = :warehouseId AND s.status IN (mexa.club.warehouseproject.entity.InventoryStatus.OPEN, mexa.club.warehouseproject.entity.InventoryStatus.IN_PROGRESS)")
    int countActiveSessions(@Param("warehouseId") UUID warehouseId);

    @Modifying
    @Query("DELETE FROM InventoryItem i WHERE i.session.id IN (SELECT s.id FROM InventorySession s WHERE s.warehouseId = :warehouseId)")
    void deleteItemsByWarehouseId(@Param("warehouseId") UUID warehouseId);

    @Modifying
    @Query("DELETE FROM InventorySession s WHERE s.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") UUID warehouseId);
}
