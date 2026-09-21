package mexa.club.shopservice.service;

import mexa.club.shopservice.dto.AdminReviewResponse;
import mexa.club.shopservice.dto.CreateReviewRequest;
import mexa.club.shopservice.dto.MyReviewResponse;
import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.dto.PublicReviewResponse;
import mexa.club.shopservice.dto.RatingStatsResponse;
import mexa.club.shopservice.dto.RecommendedProductResponse;
import mexa.club.shopservice.dto.ReviewPageResponse;
import mexa.club.shopservice.dto.order.OrderStatusPayload;
import mexa.club.shopservice.entity.ProductReview;
import mexa.club.shopservice.exception.ProductNotFoundException;
import mexa.club.shopservice.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ProductReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewService.class);

    private final ProductReviewRepository reviewRepository;
    private final ProductCatalogService   productCatalogService;
    private final OrderProxyService       orderProxyService;

    public ProductReviewService(ProductReviewRepository reviewRepository,
                                ProductCatalogService productCatalogService,
                                OrderProxyService orderProxyService) {
        this.reviewRepository      = reviewRepository;
        this.productCatalogService = productCatalogService;
        this.orderProxyService     = orderProxyService;
    }

    // ── Public reviews list ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReviewPageResponse listPublic(UUID productId, int page, int size) {
        Page<ProductReview> p = reviewRepository.findAllByProductIdOrderByCreatedAtDesc(
                productId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))
        );
        return new ReviewPageResponse(
                p.getContent().stream().map(this::toPublic).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }

    // ── Rating statistics ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RatingStatsResponse getRatingStats(UUID productId) {
        long   count = reviewRepository.countByProductId(productId);
        double avg   = count > 0 ? reviewRepository.findAvgRatingByProductId(productId) : 0.0;

        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        reviewRepository.findRatingDistribution(productId)
                .forEach(row -> dist.put(((Number) row[0]).intValue(),
                                         ((Number) row[1]).longValue()));

        double rounded = Math.round(avg * 10.0) / 10.0;
        return new RatingStatsResponse(productId, rounded, count, dist);
    }

    /** Bir nechta mahsulot uchun reytingni batch qilib olish (product-service uchun). */
    @Transactional(readOnly = true)
    public Map<UUID, double[]> getRatingBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.findRatingBatch(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> new double[]{
                                Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0,
                                ((Number) row[2]).doubleValue()
                        }
                ));
    }

    // ── My review for a product ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<MyReviewResponse> getMyReview(UUID userId, UUID productId) {
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(r -> toMyResponse(r, true));
    }

    // ── Create or update review ───────────────────────────────────────────────

    @Transactional
    public MyReviewResponse createOrUpdate(UUID userId, String username,
                                           String authHeader, CreateReviewRequest req) {
        // 1. Mahsulot mavjudligini tekshirish
        productCatalogService.getProductById(req.productId());

        // 2. Sotib olinganligini tekshirish
        if (!hasPurchasedProduct(authHeader, req.productId())) {
            throw new IllegalStateException(
                    "Faqat sotib olingan mahsulotga baho berish mumkin. " +
                    "Mahsulotni buyurtma qiling va yetkazib berilgandan so'ng baho bering.");
        }

        // 3. Mavjud reviewni yangilash yoki yangi yaratish
        ProductReview row = reviewRepository.findByUserIdAndProductId(userId, req.productId())
                .orElseGet(() -> {
                    ProductReview r = new ProductReview();
                    r.setUserId(userId);
                    r.setProductId(req.productId());
                    return r;
                });

        row.setRating((short) req.rating());
        row.setComment(trim(req.comment()));
        row.setUsername(username);

        return toMyResponse(reviewRepository.save(row), true);
    }

    // ── Recommendations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecommendedProductResponse> getRecommended(int limit, boolean discountOnly) {
        int requested = Math.min(50, Math.max(1, limit));
        // discountOnly rejimida ko'proq nomzod olinadi — chegirmalilarni to'ldirish uchun zaxira kerak
        int candidates = discountOnly ? Math.min(50, requested * 4) : requested;
        List<Object[]> topRows = reviewRepository.findTopRatedProductIds(
                1L,
                PageRequest.of(0, candidates)
        );
        if (topRows.isEmpty()) {
            return List.of();
        }

        // BATCH: bitta HTTP so'rov (N ta ketma-ket getProductById o'rniga) — homepage tezligi uchun kritik
        List<UUID> ids = topRows.stream().map(row -> (UUID) row[0]).toList();
        Map<UUID, ProductResponse> byId = new HashMap<>();
        try {
            for (ProductResponse p : productCatalogService.getProductsByIds(ids)) {
                byId.putIfAbsent(p.id(), p);
            }
        } catch (Exception e) {
            log.warn("Recommended batch fetch error: {}", e.getMessage());
        }

        List<RecommendedProductResponse> result = new ArrayList<>();
        for (Object[] row : topRows) {
            UUID productId = (UUID) row[0];
            double avgRating = ((Number) row[1]).doubleValue();
            long cnt = ((Number) row[2]).longValue();
            double rounded = Math.round(avgRating * 10.0) / 10.0;
            ProductResponse p = byId.get(productId);
            if (p == null) {
                // Batch'da topilmaganlar uchun yakka fallback — robustlik saqlanadi
                try {
                    p = productCatalogService.getProductById(productId);
                } catch (Exception e) {
                    log.warn("Recommended product {} fetch error: {}", productId, e.getMessage());
                    continue;
                }
            }
            if (!p.active()) {
                continue;
            }
            // discountOnly: faqat chegirma 0 dan farqli (faol chegirmali) mahsulotlar chiqadi
            if (discountOnly && (p.discountPercent() == null || p.discountPercent() == 0)) {
                continue;
            }
            String thumb = (p.imageUrls() != null && !p.imageUrls().isEmpty())
                    ? p.imageUrls().get(0) : null;
            result.add(new RecommendedProductResponse(
                    productId, p.name(), thumb,
                    p.categoryName(), p.brandName(),
                    p.basePrice() != null ? p.basePrice().doubleValue() : null,
                    rounded, cnt
            ));
            if (result.size() >= requested) {
                break;
            }
        }
        return result;
    }

    // ── Purchase verification ─────────────────────────────────────────────────

    private boolean hasPurchasedProduct(String authHeader, UUID productId) {
        try {
            for (int page = 0; page < 3; page++) {
                var result = orderProxyService.listMyOrders(authHeader, page, 20);
                if (result == null || result.content() == null || result.content().isEmpty()) break;

                boolean found = result.content().stream()
                        .filter(o -> o.status() == OrderStatusPayload.DELIVERED)
                        .flatMap(o -> o.items() != null ? o.items().stream() : Stream.empty())
                        .anyMatch(item -> productId.equals(item.productId()));

                if (found) return true;
                if (page >= result.totalPages() - 1) break;
            }
        } catch (Exception e) {
            // Order-service javob bermasa — review yozishga ruxsat beriladi
            log.warn("Purchase check unavailable for product {}: {}", productId, e.getMessage());
            return true;
        }
        return false;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private PublicReviewResponse toPublic(ProductReview r) {
        return new PublicReviewResponse(
                r.getId(),
                r.getUsername() != null ? r.getUsername() : "Foydalanuvchi",
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private MyReviewResponse toMyResponse(ProductReview r, boolean canEdit) {
        return new MyReviewResponse(
                r.getId(), r.getProductId(), r.getRating(), r.getComment(),
                r.getCreatedAt(), r.getUpdatedAt(), canEdit
        );
    }

    // ── Admin methods ─────────────────────────────────────────────────────────

    /** Barcha sharhlarni sahifalab qaytaradi (admin uchun). productId bo'lsa filter qiladi */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AdminReviewResponse> listAllAdmin(
            UUID productId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<ProductReview> p = (productId != null)
                ? reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId, pr)
                : reviewRepository.findAll(pr.withSort(
                        org.springframework.data.domain.Sort.by("createdAt").descending()));
        return p.map(this::toAdminResponse);
    }

    /** Admin: sharhni o'chirish */
    @Transactional
    public void deleteReview(UUID reviewId) {
        ProductReview r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ProductNotFoundException(reviewId));
        reviewRepository.delete(r);
    }

    /** Mahsulotning barcha reviewlarini o'chirish (product o'chirilganda). */
    @Transactional
    public void deleteByProductId(UUID productId) {
        reviewRepository.deleteByProductId(productId);
    }

    private AdminReviewResponse toAdminResponse(ProductReview r) {
        return new AdminReviewResponse(
                r.getId(), r.getUserId(), r.getProductId(),
                r.getUsername() != null ? r.getUsername() : "—",
                r.getRating(), r.getComment(),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    private static String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
