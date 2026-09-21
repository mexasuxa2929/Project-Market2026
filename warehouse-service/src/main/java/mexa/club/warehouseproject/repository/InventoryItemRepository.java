package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findBySession_IdOrderByProductIdAsc(UUID sessionId);
    Optional<InventoryItem> findByIdAndSession_Id(UUID id, UUID sessionId);
}
