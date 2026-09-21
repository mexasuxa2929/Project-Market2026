package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:targetType IS NULL OR a.targetType = :targetType)
              AND (:targetId IS NULL OR a.targetId = :targetId)
              AND (:warehouseId IS NULL OR a.warehouseId = :warehouseId)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    Page<AuditLog> search(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("warehouseId") UUID warehouseId,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM AuditLog a WHERE a.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") UUID warehouseId);
}
