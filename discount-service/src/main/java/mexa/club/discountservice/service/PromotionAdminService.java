package mexa.club.discountservice.service;

import mexa.club.discountservice.dto.CreatePromotionRequest;
import mexa.club.discountservice.dto.PageResponse;
import mexa.club.discountservice.dto.PromotionResponse;
import mexa.club.discountservice.dto.PromotionStatsResponse;
import mexa.club.discountservice.dto.PromotionUsageResponse;
import mexa.club.discountservice.dto.UpdatePromotionRequest;
import mexa.club.discountservice.entity.Promotion;
import mexa.club.discountservice.entity.PromotionType;
import mexa.club.discountservice.entity.PromotionUsage;
import mexa.club.discountservice.exception.DiscountException;
import mexa.club.discountservice.repository.PromotionRepository;
import mexa.club.discountservice.repository.PromotionUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.UUID;

@Service
public class PromotionAdminService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionStructMapper promotionStructMapper;

    public PromotionAdminService(
            PromotionRepository promotionRepository,
            PromotionUsageRepository promotionUsageRepository,
            PromotionStructMapper promotionStructMapper
    ) {
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.promotionStructMapper = promotionStructMapper;
    }

    @Transactional
    public PromotionResponse create(CreatePromotionRequest req) {
        String code = normalizeCode(req.code());
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new DiscountException(HttpStatus.CONFLICT, "DUPLICATE_CODE", "Promotion code already exists");
        }
        validateDateRange(req.startsAt(), req.endsAt());

        Promotion p = new Promotion();
        p.setCode(code);
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setType(req.type());
        p.setValue(req.value());
        p.setMinOrderAmount(req.minOrderAmount());
        p.setMaxDiscountAmount(req.maxDiscountAmount());
        p.setUsageLimit(req.usageLimit());
        p.setPerUserLimit(req.perUserLimit() != null ? req.perUserLimit() : 1);
        p.setStartsAt(req.startsAt());
        p.setEndsAt(req.endsAt());
        p.setActive(req.active() == null || req.active());
        applyRestrictions(p, req.restrictedProductIds(), req.restrictedCategoryIds());

        Promotion saved = promotionRepository.save(p);
        return promotionStructMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> list(Boolean active, PromotionType type, String search, int page, int size) {
        Specification<Promotion> spec = PromotionSpecifications.combined(active, type, search);
        Page<Promotion> result = promotionRepository.findAll(spec, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return new PageResponse<>(
                result.getContent().stream().map(promotionStructMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PromotionResponse get(UUID id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion not found"));
        return promotionStructMapper.toResponse(p);
    }

    @Transactional
    public PromotionResponse update(UUID id, UpdatePromotionRequest req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion not found"));

        if (req.name() != null) {
            p.setName(req.name().trim());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.type() != null) {
            p.setType(req.type());
        }
        if (req.value() != null) {
            p.setValue(req.value());
        }
        if (req.minOrderAmount() != null) {
            p.setMinOrderAmount(req.minOrderAmount());
        }
        if (req.maxDiscountAmount() != null) {
            p.setMaxDiscountAmount(req.maxDiscountAmount());
        }
        if (req.usageLimit() != null) {
            p.setUsageLimit(req.usageLimit());
        }
        if (req.perUserLimit() != null) {
            p.setPerUserLimit(req.perUserLimit());
        }
        if (req.startsAt() != null) {
            p.setStartsAt(req.startsAt());
        }
        if (req.endsAt() != null) {
            p.setEndsAt(req.endsAt());
        }
        if (req.active() != null) {
            p.setActive(req.active());
        }
        validateDateRange(p.getStartsAt(), p.getEndsAt());

        if (req.restrictedProductIds() != null) {
            p.getRestrictedProductIds().clear();
            p.getRestrictedProductIds().addAll(req.restrictedProductIds());
        }
        if (req.restrictedCategoryIds() != null) {
            p.getRestrictedCategoryIds().clear();
            p.getRestrictedCategoryIds().addAll(req.restrictedCategoryIds());
        }

        return promotionStructMapper.toResponse(promotionRepository.save(p));
    }

    @Transactional
    public void softDelete(UUID id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND,
                        "Promotion not found"));
        p.setActive(false);
        promotionRepository.save(p);
    }

    @Transactional
    public void deactivate(UUID id) {
        softDelete(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionUsageResponse> usages(UUID promotionId, int page, int size) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new DiscountException(HttpStatus.NOT_FOUND, DiscountException.PROMOTION_NOT_FOUND, "Promotion not found");
        }
        Page<PromotionUsage> result = promotionUsageRepository.findByPromotionIdOrderByUsedAtDesc(
                promotionId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))
        );
        return new PageResponse<>(
                result.getContent().stream().map(promotionStructMapper::toUsage).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PromotionStatsResponse stats() {
        long totalPromotions = promotionRepository.count();
        BigDecimal sum = promotionUsageRepository.sumAllDiscountAmounts();
        if (sum == null) {
            sum = BigDecimal.ZERO;
        }

        Page<Object[]> topPage = promotionUsageRepository.promotionIdsByUsageDesc(PageRequest.of(0, 1));
        UUID topId = null;
        String topCode = null;
        long topUsage = 0;
        if (topPage.hasContent()) {
            Object[] row = topPage.getContent().get(0);
            topId = (UUID) row[0];
            topUsage = ((Number) row[1]).longValue();
            topCode = promotionRepository.findById(topId).map(Promotion::getCode).orElse(null);
        }

        return new PromotionStatsResponse(totalPromotions, sum, topId, topCode, topUsage);
    }

    private static void validateDateRange(java.time.LocalDateTime starts, java.time.LocalDateTime ends) {
        if (ends != null && ends.isBefore(starts)) {
            throw new DiscountException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "endsAt must be after startsAt");
        }
    }

    private static void applyRestrictions(Promotion p, java.util.Set<UUID> products, java.util.Set<UUID> categories) {
        p.getRestrictedProductIds().clear();
        if (products != null) {
            p.getRestrictedProductIds().addAll(products);
        }
        p.getRestrictedCategoryIds().clear();
        if (categories != null) {
            p.getRestrictedCategoryIds().addAll(categories);
        }
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
