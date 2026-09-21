package mexa.club.discountservice.service;

import feign.FeignException;
import mexa.club.discountservice.client.ProductCatalogClient;
import mexa.club.discountservice.client.dto.ProductCatalogPayload;
import mexa.club.discountservice.dto.ApiResponse;
import mexa.club.discountservice.dto.ApplyDiscountRequest;
import mexa.club.discountservice.dto.ApplyDiscountResponse;
import mexa.club.discountservice.dto.InternalDiscountApplyRequest;
import mexa.club.discountservice.dto.InternalDiscountConfirmRequest;
import mexa.club.discountservice.dto.PublicPromotionResponse;
import mexa.club.discountservice.entity.Promotion;
import mexa.club.discountservice.entity.PromotionType;
import mexa.club.discountservice.entity.PromotionUsage;
import mexa.club.discountservice.exception.DiscountException;
import mexa.club.discountservice.repository.PromotionRepository;
import mexa.club.discountservice.repository.PromotionUsageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DiscountService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ProductCatalogClient productCatalogClient;
    private final PromotionStructMapper promotionStructMapper;

    public DiscountService(
            PromotionRepository promotionRepository,
            PromotionUsageRepository promotionUsageRepository,
            ProductCatalogClient productCatalogClient,
            PromotionStructMapper promotionStructMapper
    ) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.productCatalogClient = productCatalogClient;
        this.promotionStructMapper = promotionStructMapper;
    }

    @Transactional(readOnly = true)
    public ApplyDiscountResponse applyPreview(UUID userId, ApplyDiscountRequest request) {
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(request.code())
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion not found for code"));
        List<UUID> mergedCategories = enrichCategories(request.productIds(), request.categoryIds());
        validatePromotion(promotion, userId, request.orderAmount(), request.productIds(), mergedCategories);
        return computeResponse(promotion, request.orderAmount());
    }

    @Transactional
    public ApplyDiscountResponse internalApplyAndRecord(InternalDiscountApplyRequest req) {
        UUID orderId = req.orderId() != null ? req.orderId() : UUID.randomUUID();
        if (promotionUsageRepository.existsByOrderId(orderId)) {
            throw new DiscountException(HttpStatus.CONFLICT, DiscountException.ORDER_ALREADY_DISCOUNTED,
                    "Order already has a recorded discount usage");
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCaseForUpdate(req.code())
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion not found for code"));

        List<UUID> mergedCategories = enrichCategories(req.productIds(), req.categoryIds());
        validatePromotion(promotion, req.userId(), req.orderAmount(), req.productIds(), mergedCategories);

        ApplyDiscountResponse computed = computeResponse(promotion, req.orderAmount());

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(promotion);
        usage.setUserId(req.userId());
        usage.setOrderId(orderId);
        usage.setDiscountAmount(computed.discountAmount());
        promotionUsageRepository.save(usage);

        promotion.setUsageCount(promotion.getUsageCount() + 1);
        if (promotion.getUsageLimit() != null && promotion.getUsageCount() >= promotion.getUsageLimit()) {
            promotion.setActive(false);
        }
        promotionRepository.save(promotion);

        return computed;
    }

    @Transactional(readOnly = true)
    public void confirmUsage(InternalDiscountConfirmRequest req) {
        PromotionUsage usage = promotionUsageRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.USAGE_NOT_FOUND,
                        "Discount usage not found for order"));

        if (!usage.getUserId().equals(req.userId())) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.CONFIRM_MISMATCH,
                    "User does not match discount usage");
        }
        if (!usage.getPromotion().getCode().equalsIgnoreCase(req.code())) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.CONFIRM_MISMATCH,
                    "Promotion code mismatch");
        }
        if (usage.getDiscountAmount().compareTo(req.discountAmount()) != 0) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.CONFIRM_MISMATCH,
                    "Discount amount mismatch");
        }
    }

    @Transactional
    public void cancelUsage(UUID orderId) {
        PromotionUsage usage = promotionUsageRepository.findByOrderId(orderId).orElse(null);
        if (usage == null) {
            return;
        }
        Promotion promotion = promotionRepository.findById(usage.getPromotion().getId())
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion missing"));
        promotionUsageRepository.delete(usage);
        promotion.setUsageCount(Math.max(0, promotion.getUsageCount() - 1));
        promotionRepository.save(promotion);
    }

    @Transactional(readOnly = true)
    public List<PublicPromotionResponse> listActivePublic() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findPublicActive(now).stream()
                .map(promotionStructMapper::toPublic)
                .toList();
    }

    private List<UUID> enrichCategories(List<UUID> productIds, List<UUID> categoryIdsFromClient) {
        Set<UUID> merged = new LinkedHashSet<>(categoryIdsFromClient);
        for (UUID productId : productIds) {
            try {
                ApiResponse<ProductCatalogPayload> wrap = productCatalogClient.getProduct(productId);
                if (wrap != null && wrap.success()) {
                    ProductCatalogPayload body = wrap.data();
                    if (body != null && body.categoryId() != null) {
                        merged.add(body.categoryId());
                    }
                }
            } catch (FeignException.NotFound e) {
                // ignore missing product for category enrichment
            } catch (FeignException e) {
                throw new DiscountException(HttpStatus.BAD_GATEWAY, "PRODUCT_SERVICE_ERROR",
                        "Product service error while resolving categories");
            }
        }
        return new ArrayList<>(merged);
    }

    private void validatePromotion(
            Promotion p,
            UUID userId,
            BigDecimal orderAmount,
            List<UUID> productIds,
            List<UUID> enrichedCategoryIds
    ) {
        if (!p.isActive()) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.PROMOTION_INACTIVE,
                    "Promotion is inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (p.getStartsAt().isAfter(now)) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.PROMOTION_NOT_STARTED,
                    "Promotion has not started yet");
        }
        if (p.getEndsAt() != null && p.getEndsAt().isBefore(now)) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.PROMOTION_EXPIRED,
                    "Promotion has expired");
        }

        if (p.getUsageLimit() != null && p.getUsageCount() >= p.getUsageLimit()) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.USAGE_LIMIT_REACHED,
                    "Promotion usage limit reached");
        }

        int perUser = p.getPerUserLimit() == null ? 1 : p.getPerUserLimit();
        long userUses = promotionUsageRepository.countByPromotionIdAndUserId(p.getId(), userId);
        if (userUses >= perUser) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.USER_LIMIT_REACHED,
                    "Per-user promotion limit reached");
        }

        if (p.getMinOrderAmount() != null && orderAmount.compareTo(p.getMinOrderAmount()) < 0) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.MIN_ORDER_AMOUNT_NOT_MET,
                    "Minimum order amount not met");
        }

        boolean prodEmpty = p.getRestrictedProductIds().isEmpty();
        boolean catEmpty = p.getRestrictedCategoryIds().isEmpty();
        if (!prodEmpty || !catEmpty) {
            boolean okProd = prodEmpty || productIds.stream().anyMatch(pid -> p.getRestrictedProductIds().contains(pid));
            boolean okCat = catEmpty || enrichedCategoryIds.stream().anyMatch(cid -> p.getRestrictedCategoryIds().contains(cid));

            boolean eligible;
            if (!prodEmpty && catEmpty) {
                eligible = okProd;
            } else if (prodEmpty) {
                eligible = okCat;
            } else {
                eligible = okProd || okCat;
            }

            if (!eligible) {
                throw new DiscountException(HttpStatus.BAD_REQUEST, DiscountException.PRODUCT_NOT_ELIGIBLE,
                        "Cart does not contain eligible products or categories");
            }
        }
    }

    private ApplyDiscountResponse computeResponse(Promotion p, BigDecimal orderAmount) {
        BigDecimal discount;
        boolean freeShipping = false;
        String message;
        switch (p.getType()) {
            case PERCENTAGE -> {
                discount = orderAmount.multiply(p.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (p.getMaxDiscountAmount() != null) {
                    discount = discount.min(p.getMaxDiscountAmount());
                }
                discount = discount.min(orderAmount).max(BigDecimal.ZERO);
                message = "Percentage discount applied";
            }
            case FIXED_AMOUNT -> {
                discount = p.getValue().min(orderAmount).max(BigDecimal.ZERO);
                message = "Fixed amount discount applied";
            }
            case FREE_SHIPPING -> {
                discount = BigDecimal.ZERO;
                freeShipping = true;
                message = "Free shipping promotion — delivery fee should be waived downstream";
            }
            default -> throw new IllegalStateException("Unsupported promotion type");
        }

        BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);
        return new ApplyDiscountResponse(
                p.getId(),
                p.getCode(),
                p.getType(),
                discount.setScale(2, RoundingMode.HALF_UP),
                finalAmount.setScale(2, RoundingMode.HALF_UP),
                freeShipping,
                message
        );
    }
}
