package mexa.club.warehouseproject.repository;

import jakarta.persistence.criteria.Predicate;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    default Specification<StockMovement> filterSpec(UUID warehouseId, UUID productId, MovementType movementType, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            if (productId != null) predicates.add(cb.equal(root.get("productId"), productId));
            if (movementType != null) predicates.add(cb.equal(root.get("movementType"), movementType));
            if (dateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            if (dateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    List<StockMovement> findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(
            UUID warehouseId,
            String referenceType,
            UUID referenceId,
            MovementType movementType
    );

    @Query("""
            SELECT m.warehouseId, COUNT(m)
            FROM StockMovement m
            WHERE m.warehouseId IN :warehouseIds
              AND m.movementType = :movementType
              AND m.createdAt >= :from
            GROUP BY m.warehouseId
            """)
    List<Object[]> countByWarehouseIdInAndMovementTypeSince(
            @Param("warehouseIds") Collection<UUID> warehouseIds,
            @Param("movementType") MovementType movementType,
            @Param("from") LocalDateTime from
    );

    @Modifying
    @Query("DELETE FROM StockMovement m WHERE m.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") UUID warehouseId);
}
