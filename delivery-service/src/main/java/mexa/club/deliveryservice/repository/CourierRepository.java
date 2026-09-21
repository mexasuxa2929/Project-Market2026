package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierRepository extends JpaRepository<Courier, UUID> {
    Optional<Courier> findByUserId(UUID userId);

    @Query("""
            SELECT c FROM Courier c WHERE
            (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.region) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:active IS NULL OR c.active = :active)
            AND (:region IS NULL OR LOWER(c.region) = LOWER(:region))
            ORDER BY c.name ASC
            """)
    List<Courier> findByFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("region") String region);

    List<Courier> findByActiveTrue();
}
