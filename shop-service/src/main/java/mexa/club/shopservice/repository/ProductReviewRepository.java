package mexa.club.shopservice.repository;

import mexa.club.shopservice.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    void deleteByProductId(UUID productId);

    Page<ProductReview> findAllByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    Optional<ProductReview> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    long countByProductId(UUID productId);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0) FROM ProductReview r WHERE r.productId = :productId")
    double findAvgRatingByProductId(@Param("productId") UUID productId);

    /** Har bir yulduz darajasi uchun nechta review borligini qaytaradi: [rating, count] */
    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r WHERE r.productId = :productId GROUP BY r.rating")
    List<Object[]> findRatingDistribution(@Param("productId") UUID productId);

    /**
     * Rekomendatsiya uchun: kamida minReviews ta review bo'lgan va
     * o'rtacha reytingi eng yuqori mahsulotlar.
     * Qaytaradi: [productId, avgRating, reviewCount]
     */
    @Query("""
            SELECT r.productId, AVG(CAST(r.rating AS double)) as avgRating, COUNT(r) as cnt
            FROM ProductReview r
            GROUP BY r.productId
            HAVING COUNT(r) >= :minReviews
            ORDER BY avgRating DESC, cnt DESC
            """)
    List<Object[]> findTopRatedProductIds(@Param("minReviews") long minReviews, Pageable pageable);

    /** Bir nechta mahsulot uchun reytingni batch qilib olish: [productId, avgRating, reviewCount] */
    @Query("""
            SELECT r.productId, AVG(CAST(r.rating AS double)) as avgRating, COUNT(r) as cnt
            FROM ProductReview r
            WHERE r.productId IN :productIds
            GROUP BY r.productId
            """)
    List<Object[]> findRatingBatch(@Param("productIds") List<UUID> productIds);
}
