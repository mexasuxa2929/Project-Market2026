package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.StockTransfer;
import mexa.club.warehouseproject.entity.StockTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {

    Optional<StockTransfer> findByIdAndToWarehouseId(UUID id, UUID toWarehouseId);

    List<StockTransfer> findByToWarehouseIdAndStatusOrderByCreatedAtDesc(UUID toWarehouseId, StockTransferStatus status);

    Page<StockTransfer> findByToWarehouseIdOrderByCreatedAtDesc(UUID toWarehouseId, Pageable pageable);

    long countByToWarehouseIdAndStatus(UUID toWarehouseId, StockTransferStatus status);
}
