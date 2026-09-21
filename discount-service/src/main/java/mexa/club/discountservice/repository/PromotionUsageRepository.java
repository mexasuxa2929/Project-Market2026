package mexa.club.discountservice.repository;

import mexa.club.discountservice.entity.PromotionUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {

    Optional<PromotionUsage> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    long countByPromotionIdAndUserId(UUID promotionId, UUID userId);

    Page<PromotionUsage> findByPromotionIdOrderByUsedAtDesc(UUID promotionId, Pageable pageable);

    @Query("select coalesce(sum(u.discountAmount), 0) from PromotionUsage u")
    BigDecimal sumAllDiscountAmounts();

    @Query("select u.promotion.id, count(u) from PromotionUsage u group by u.promotion.id order by count(u) desc")
    Page<Object[]> promotionIdsByUsageDesc(Pageable pageable);

    @Query("select coalesce(sum(u.discountAmount), 0) from PromotionUsage u where u.usedAt between :from and :to")
    BigDecimal sumDiscountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select p.code, count(u), coalesce(sum(u.discountAmount), 0)
            from PromotionUsage u join u.promotion p
            where u.usedAt between :from and :to
            group by p.id, p.code
            order by count(u) desc
            """)
    List<Object[]> topPromotionsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
