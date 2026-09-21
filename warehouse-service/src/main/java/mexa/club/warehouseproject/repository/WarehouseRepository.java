package mexa.club.warehouseproject.repository;

import mexa.club.warehouseproject.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    @EntityGraph(attributePaths = {"adminAssignments"})
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
    Optional<Warehouse> findDetailById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"adminAssignments"})
    @Query(
            value = "SELECT w FROM Warehouse w",
            countQuery = "SELECT COUNT(w) FROM Warehouse w"
    )
    Page<Warehouse> findAllWithAdmins(Pageable pageable);

    @EntityGraph(attributePaths = {"adminAssignments"})
    @Query(
            value = "SELECT DISTINCT w FROM Warehouse w JOIN w.adminAssignments a WHERE a.userId = :userId",
            countQuery = "SELECT COUNT(DISTINCT w) FROM Warehouse w JOIN w.adminAssignments a WHERE a.userId = :userId"
    )
    Page<Warehouse> findByAdminUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT w FROM Warehouse w WHERE w.active = true")
    List<Warehouse> findAllActive();
}
