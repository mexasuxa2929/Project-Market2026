package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.OperatingExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface OperatingExpenseRepository extends JpaRepository<OperatingExpense, UUID> {

    @Query("select coalesce(sum(e.amount),0) from OperatingExpense e where e.warehouse.id = :warehouseId and e.incurredAt between :from and :to")
    BigDecimal sumByWarehouseAndDate(@Param("warehouseId") UUID warehouseId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    Page<OperatingExpense> findByWarehouseId(UUID warehouseId, Pageable pageable);

    java.util.Optional<OperatingExpense> findByIdAndWarehouseId(UUID id, UUID warehouseId);
}
