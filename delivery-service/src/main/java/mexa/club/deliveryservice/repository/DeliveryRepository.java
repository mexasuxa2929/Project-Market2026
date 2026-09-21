package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.Delivery;
import mexa.club.deliveryservice.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByTrackingCode(String trackingCode);
    Optional<Delivery> findByOrderId(UUID orderId);
    List<Delivery> findByCourierIdOrderByCreatedAtDesc(UUID courierId);
    Page<Delivery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT d FROM Delivery d WHERE
            (:status IS NULL OR d.status = :status)
            AND (:courierId IS NULL OR d.courierId = :courierId)
            AND (:dateFrom IS NULL OR d.createdAt >= :dateFrom)
            AND (:dateTo IS NULL OR d.createdAt <= :dateTo)
            AND (:region IS NULL OR d.region LIKE %:region%)
            AND (:search IS NULL OR d.recipientName LIKE %:search%
                OR d.recipientPhone LIKE %:search%
                OR d.deliveryAddress LIKE %:search%
                OR d.trackingCode LIKE %:search%)
            ORDER BY d.createdAt DESC
            """)
    Page<Delivery> findByFilters(
            @Param("status") DeliveryStatus status,
            @Param("courierId") UUID courierId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("region") String region,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.createdAt >= :since")
    long countDeliveriesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    long countByStatus(@Param("status") DeliveryStatus status);
}
