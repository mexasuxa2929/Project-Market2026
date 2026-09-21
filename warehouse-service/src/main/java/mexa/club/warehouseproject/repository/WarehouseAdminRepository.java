package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.WarehouseAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseAdminRepository extends JpaRepository<WarehouseAdmin, UUID> {

    boolean existsByWarehouse_IdAndUserId(UUID warehouseId, UUID userId);

    long deleteByWarehouse_Id(UUID warehouseId);

    @Query("SELECT DISTINCT a.userId FROM WarehouseAdmin a")
    List<UUID> findAllDistinctUserIds();

    @Query("SELECT DISTINCT a.userId FROM WarehouseAdmin a WHERE a.warehouse.id = :warehouseId")
    List<UUID> findAdminUserIdsByWarehouseId(UUID warehouseId);
}
