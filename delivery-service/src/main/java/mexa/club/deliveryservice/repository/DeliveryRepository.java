package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.Delivery;
import mexa.club.deliveryservice.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository
        extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {
    Optional<Delivery> findByTrackingCode(String trackingCode);
    Optional<Delivery> findByOrderId(UUID orderId);
    List<Delivery> findByCourierIdOrderByCreatedAtDesc(UUID courierId);
    List<Delivery> findByCourierIdIn(Collection<UUID> courierIds);
    Page<Delivery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Eski dinamik @Query findByFilters olib tashlandi: `(:param IS NULL OR ...)`
    // shaklida Postgres null parametr tipini aniqlay olmasligi (42P18) sababli
    // filtrlangan so'rovlar 500 edi. Endi DeliveryService'da Specification bilan quriladi.

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.createdAt >= :since")
    long countDeliveriesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    long countByStatus(@Param("status") DeliveryStatus status);
}
