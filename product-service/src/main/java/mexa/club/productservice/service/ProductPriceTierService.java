package mexa.club.productservice.service;

import mexa.club.productservice.dto.ProductPriceTierBulkRequest;
import mexa.club.productservice.dto.ProductPriceTierRequest;
import mexa.club.productservice.dto.ProductPriceTierResponse;
import mexa.club.productservice.entity.ProductPriceTier;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.repository.ProductPriceTierRepository;
import mexa.club.productservice.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductPriceTierService {

    private final ProductPriceTierRepository tierRepository;
    private final ProductRepository         productRepository;

    public ProductPriceTierService(ProductPriceTierRepository tierRepository,
                                   ProductRepository productRepository) {
        this.tierRepository  = tierRepository;
        this.productRepository = productRepository;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductPriceTierResponse> listForProduct(UUID productId) {
        requireProduct(productId);
        List<ProductPriceTier> tiers = tierRepository.findByProductIdOrderByMinQtyAsc(productId);
        return ProductPriceTierResponse.fromEntityList(tiers);
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductPriceTierResponse add(UUID productId, ProductPriceTierRequest req) {
        requireProduct(productId);
        validateRange(req.getMinQty(), req.getMaxQty());
        checkOverlap(productId, null, req.getMinQty(), req.getMaxQty());

        ProductPriceTier tier = new ProductPriceTier();
        tier.setProductId(productId);
        applyRequest(req, tier);
        tierRepository.save(tier);

        // Ro'yxatdagi indeksni hisoblash uchun qayta yuklaymiz
        List<ProductPriceTier> all = tierRepository.findByProductIdOrderByMinQtyAsc(productId);
        int idx = all.stream().map(ProductPriceTier::getId).toList().indexOf(tier.getId()) + 1;
        BigDecimal base = all.isEmpty() ? null : all.get(0).getPrice();
        return ProductPriceTierResponse.fromEntity(tier, idx, idx == 1 ? null : base);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public ProductPriceTierResponse update(UUID productId, UUID tierId, ProductPriceTierRequest req) {
        requireProduct(productId);
        ProductPriceTier tier = tierRepository.findById(tierId)
                .filter(t -> t.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("PriceTier", tierId.toString()));

        validateRange(req.getMinQty(), req.getMaxQty());
        checkOverlap(productId, tierId, req.getMinQty(), req.getMaxQty());

        applyRequest(req, tier);
        tierRepository.save(tier);

        List<ProductPriceTier> all = tierRepository.findByProductIdOrderByMinQtyAsc(productId);
        int idx = all.stream().map(ProductPriceTier::getId).toList().indexOf(tier.getId()) + 1;
        BigDecimal base = all.isEmpty() ? null : all.get(0).getPrice();
        return ProductPriceTierResponse.fromEntity(tier, idx, idx == 1 ? null : base);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public void delete(UUID productId, UUID tierId) {
        requireProduct(productId);
        ProductPriceTier tier = tierRepository.findById(tierId)
                .filter(t -> t.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("PriceTier", tierId.toString()));
        tierRepository.delete(tier);
    }

    // ── Bulk replace ──────────────────────────────────────────────────────────

    /**
     * Mavjud barcha tier larni o'chirib, yangilarini saqlaydi.
     * Frontend yangi mahsulot yaratganda yoki to'liq almashtirish kerak bo'lganda ishlatadi.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    @Transactional
    public List<ProductPriceTierResponse> bulkReplace(UUID productId, ProductPriceTierBulkRequest req) {
        requireProduct(productId);

        List<ProductPriceTierRequest> incoming = req.getTiers();

        // Har birini validate qil
        for (int i = 0; i < incoming.size(); i++) {
            ProductPriceTierRequest t = incoming.get(i);
            validateRange(t.getMinQty(), t.getMaxQty());
        }

        // Ichki kesishuvlarni tekshir
        validateNoInternalOverlap(incoming);

        // O'chirish va qayta saqlash
        tierRepository.deleteByProductId(productId);

        List<ProductPriceTier> saved = new ArrayList<>();
        for (ProductPriceTierRequest r : incoming) {
            ProductPriceTier tier = new ProductPriceTier();
            tier.setProductId(productId);
            applyRequest(r, tier);
            saved.add(tierRepository.save(tier));
        }

        // minQty bo'yicha sort qilib qaytaramiz
        saved.sort(java.util.Comparator.comparingInt(ProductPriceTier::getMinQty));
        return ProductPriceTierResponse.fromEntityList(saved);
    }

    // ── Resolve price ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<BigDecimal> resolvePrice(UUID productId, int qty) {
        return tierRepository.findMatchingTier(productId, qty)
                .map(ProductPriceTier::getPrice);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId.toString());
        }
    }

    private static void validateRange(int minQty, Integer maxQty) {
        if (minQty < 0)
            throw new IllegalArgumentException("minQty 0 dan kam bo'lmaydi");
        if (maxQty != null && maxQty < minQty)
            throw new IllegalArgumentException("maxQty minQty dan kichik bo'lmasligi kerak");
    }

    /**
     * Yangi/yangilangan tier mavjud tier lar bilan kesishmasligini tekshiradi.
     * @param excludeId yangilash holati uchun — o'zini hisobga olmasin
     */
    private void checkOverlap(UUID productId, UUID excludeId, int minQty, Integer maxQty) {
        List<ProductPriceTier> existing = tierRepository.findByProductIdOrderByMinQtyAsc(productId);
        for (ProductPriceTier t : existing) {
            if (excludeId != null && t.getId().equals(excludeId)) continue;

            boolean overlaps = (maxQty == null || maxQty >= t.getMinQty())
                    && (t.getMaxQty() == null || t.getMaxQty() >= minQty);

            if (overlaps) {
                String existingRange = t.getMaxQty() == null
                        ? t.getMinQty() + "+ units"
                        : t.getMinQty() + " – " + t.getMaxQty() + " units";
                throw new IllegalArgumentException(
                        "Oraliq mavjud bosqich bilan kesishyapti: " + existingRange);
            }
        }
    }

    /** Bir so'rovdagi tier lar o'zaro kesishmasligi kerak */
    private static void validateNoInternalOverlap(List<ProductPriceTierRequest> tiers) {
        for (int i = 0; i < tiers.size(); i++) {
            for (int j = i + 1; j < tiers.size(); j++) {
                ProductPriceTierRequest a = tiers.get(i);
                ProductPriceTierRequest b = tiers.get(j);
                boolean overlaps = (a.getMaxQty() == null || a.getMaxQty() >= b.getMinQty())
                        && (b.getMaxQty() == null || b.getMaxQty() >= a.getMinQty());
                if (overlaps) {
                    throw new IllegalArgumentException(
                            "Tier " + (i + 1) + " va tier " + (j + 1) + " oralig'i kesishyapti");
                }
            }
        }
    }

    private static void applyRequest(ProductPriceTierRequest req, ProductPriceTier tier) {
        tier.setMinQty(req.getMinQty());
        tier.setMaxQty(req.getMaxQty());
        tier.setPrice(req.getPrice());
        tier.setCurrency(req.getCurrency() != null ? req.getCurrency() : "UZS");
        tier.setPriceType(req.getPriceType() != null ? req.getPriceType() : "RETAIL");
    }
}
