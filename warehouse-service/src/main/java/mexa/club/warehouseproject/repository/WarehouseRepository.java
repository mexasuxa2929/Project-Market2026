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
            value = """
                    SELECT w FROM Warehouse w
                    WHERE (:q IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.location, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.address, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:active IS NULL OR w.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(w) FROM Warehouse w
                    WHERE (:q IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.location, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.address, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:active IS NULL OR w.active = :active)
                    """
    )
    Page<Warehouse> findAllWithAdmins(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @EntityGraph(attributePaths = {"adminAssignments"})
    @Query(
            value = """
                    SELECT DISTINCT w FROM Warehouse w JOIN w.adminAssignments a
                    WHERE a.userId = :userId
                      AND (:q IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.location, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.address, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:active IS NULL OR w.active = :active)
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT w) FROM Warehouse w JOIN w.adminAssignments a
                    WHERE a.userId = :userId
                      AND (:q IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.location, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(COALESCE(w.address, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:active IS NULL OR w.active = :active)
                    """
    )
    Page<Warehouse> findByAdminUserId(
            @Param("userId") UUID userId,
            @Param("q") String q,
            @Param("active") Boolean active,
            Pageable pageable);

    /** Umumiy statistika (T-H4 — ro'yxat sahifalanadi, kartalar esa global hisoblanadi) */
    @Query("SELECT COUNT(w) FROM Warehouse w WHERE w.active = true")
    long countActive();

    @Query("SELECT COALESCE(SUM(w.capacity), 0) FROM Warehouse w")
    long sumCapacity();

    @Query("SELECT w.id FROM Warehouse w")
    List<UUID> findAllIds();

    @Query("SELECT w FROM Warehouse w WHERE w.active = true")
    List<Warehouse> findAllActive();
}
