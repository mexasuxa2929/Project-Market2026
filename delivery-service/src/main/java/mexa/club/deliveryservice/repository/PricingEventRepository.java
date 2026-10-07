package mexa.club.deliveryservice.repository;

import mexa.club.deliveryservice.entity.PricingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PricingEventRepository extends JpaRepository<PricingEvent, UUID> {

    @Query("select count(e) from PricingEvent e where e.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(case when e.freeDelivery = true then 1 else 0 end), 0) from PricingEvent e where e.createdAt >= :since")
    long countFreeSince(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(case when e.freeDelivery = true or e.discount > 0 then 1 else 0 end), 0) from PricingEvent e where e.createdAt >= :since")
    long countIncentiveSince(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(e.finalFee), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgFee(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(e.estimatedCost), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgCost(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(case when e.estimatedCost > e.finalFee then e.estimatedCost - e.finalFee else 0 end), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgSubsidy(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(e.margin), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgMargin(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(e.cartTotal), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgCart(@Param("since") LocalDateTime since);

    @Query("select coalesce(avg(e.distanceKm), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal avgDistance(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(e.finalFee), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal sumFee(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(e.estimatedCost), 0) from PricingEvent e where e.createdAt >= :since")
    BigDecimal sumCost(@Param("since") LocalDateTime since);

    /** Threshold exposure: cart >= threshold bo'lganlar soni. */
    @Query("select count(e) from PricingEvent e where e.createdAt >= :since and e.cartTotal >= :threshold")
    long countExposed(@Param("since") LocalDateTime since, @Param("threshold") BigDecimal threshold);

    /** Threshold'dan o'tganlar: bepul yetkazilgan. */
    @Query("select count(e) from PricingEvent e where e.createdAt >= :since and e.cartTotal >= :threshold and e.freeDelivery = true")
    long countReachedFree(@Param("since") LocalDateTime since, @Param("threshold") BigDecimal threshold);

    /** Threshold'dan o'tganlar: chegirma qo'llanib bepul/skidka olgan. */
    @Query("select count(e) from PricingEvent e where e.createdAt >= :since and e.cartTotal >= :threshold and e.discount > 0")
    long countReachedDiscount(@Param("since") LocalDateTime since, @Param("threshold") BigDecimal threshold);

    @Query("select coalesce(avg(e.cartTotal), 0) from PricingEvent e where e.createdAt >= :since and e.thresholdAmount = :threshold")
    BigDecimal avgCartForThreshold(@Param("since") LocalDateTime since, @Param("threshold") BigDecimal threshold);

    @Query("select e from PricingEvent e where e.createdAt >= :since order by e.createdAt desc")
    List<PricingEvent> findRecent(@Param("since") LocalDateTime since);
}
