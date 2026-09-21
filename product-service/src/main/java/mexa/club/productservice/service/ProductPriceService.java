package mexa.club.productservice.service;

import mexa.club.productservice.dto.ProductPriceRequest;
import mexa.club.productservice.dto.ProductPriceResponse;
import mexa.club.productservice.entity.Product;
import mexa.club.productservice.entity.ProductPrice;
import mexa.club.productservice.entity.ProductPriceTier;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.repository.ProductPriceRepository;
import mexa.club.productservice.repository.ProductPriceTierRepository;
import mexa.club.productservice.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductPriceService {

    private final ProductPriceRepository productPriceRepository;
    private final ProductPriceTierRepository productPriceTierRepository;
    private final ProductRepository productRepository;

    public ProductPriceService(
            ProductPriceRepository productPriceRepository,
            ProductPriceTierRepository productPriceTierRepository,
            ProductRepository productRepository
    ) {
        this.productPriceRepository = productPriceRepository;
        this.productPriceTierRepository = productPriceTierRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductPriceResponse> listForProduct(UUID productId) {
        requireProduct(productId);
        BigDecimal tier1Base = firstTierPrice(productId);
        LocalDateTime now = LocalDateTime.now();
        return productPriceRepository.findByProductIdOrderByEffectiveDateDesc(productId).stream()
                .map(e -> withRuleCurrentPrice(ProductPriceResponse.fromEntity(e), tier1Base, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductPriceResponse getPrice(UUID productId, UUID priceId) {
        requireProduct(productId);
        ProductPrice e = productPriceRepository.findByIdAndProductId(priceId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductPrice", priceId.toString()));
        return withRuleCurrentPrice(ProductPriceResponse.fromEntity(e), firstTierPrice(productId), LocalDateTime.now());
    }

    /** 1-liniya narxi — chegirma qaysi narx ustidan qo'llanadi (liniya bo'lmasa null). */
    private BigDecimal firstTierPrice(UUID productId) {
        return productPriceTierRepository.findByProductIdOrderByMinQtyAsc(productId).stream()
                .findFirst()
                .map(ProductPriceTier::getPrice)
                .orElse(null);
    }

    /**
     * Qoida: chegirma 1-liniya narxi ustidan qo'llanadi (tier1 bo'lmasa flat salePrice ustidan,
     * buni ProductPriceResponse.fromEntity allaqachon hisoblaydi).
     */
    private static ProductPriceResponse withRuleCurrentPrice(ProductPriceResponse resp, BigDecimal tier1Base, LocalDateTime now) {
        if (resp.isDiscountActive() && tier1Base != null && resp.getDiscountPercent() != null) {
            resp.setCurrentPrice(tier1Base.multiply(BigDecimal.valueOf(100 - resp.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP));
        }
        return resp;
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductPriceResponse addPrice(UUID productId, ProductPriceRequest dto) {
        requireProduct(productId);
        validateDiscount(dto);
        ProductPrice e = new ProductPrice();
        e.setProductId(productId);
        e.setSalePrice(dto.getSalePrice());
        e.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDateTime.now());
        e.setEndDate(dto.getEndDate());
        e.setDiscountPercent(dto.getDiscountPercent());
        e.setDiscountStartDate(dto.getDiscountStartDate());
        e.setDiscountEndDate(dto.getDiscountEndDate());
        ProductPrice saved = productPriceRepository.save(e);
        return ProductPriceResponse.fromEntity(saved);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductPriceResponse updatePrice(UUID productId, UUID priceId, ProductPriceRequest dto) {
        requireProduct(productId);
        ProductPrice e = productPriceRepository.findByIdAndProductId(priceId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductPrice", String.valueOf(priceId)));
        validateDiscount(dto);

        e.setSalePrice(dto.getSalePrice());
        e.setSalePrice(dto.getSalePrice());
        e.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDateTime.now());
        e.setEndDate(dto.getEndDate());

        // Chegirma bloki: discountPercent null bo'lsa mavjud chegirma saqlanadi (qisman update).
        // Shunda admin oddiy narx tahrirlash bilan faol chegirmani o'chirib yubormaydi.
        if (dto.getDiscountPercent() != null) {
            e.setDiscountPercent(dto.getDiscountPercent());
            e.setDiscountStartDate(dto.getDiscountStartDate());
            e.setDiscountEndDate(dto.getDiscountEndDate());
            if (dto.getDiscountPercent() <= 0) {
                e.setDiscountStartDate(null);
                e.setDiscountEndDate(null);
            }
        }

        ProductPrice saved = productPriceRepository.save(e);
        return ProductPriceResponse.fromEntity(saved);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public void deletePrice(UUID productId, UUID priceId) {
        requireProduct(productId);
        ProductPrice e = productPriceRepository.findByIdAndProductId(priceId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductPrice", String.valueOf(priceId)));

        productPriceRepository.delete(e);
    }

    private static void validateDiscount(ProductPriceRequest dto) {
        Integer pct = dto.getDiscountPercent();
        if (pct == null) {
            return;
        }
        if (pct < 0 || pct > 100) {
            throw new IllegalArgumentException("discountPercent must be between 0 and 100");
        }
        if (dto.getDiscountStartDate() != null && dto.getDiscountEndDate() != null
                && dto.getDiscountStartDate().isAfter(dto.getDiscountEndDate())) {
            throw new IllegalArgumentException("discountStartDate must be before discountEndDate");
        }
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", String.valueOf(productId));
        }
    }
}

