package mexa.club.shopservice.service;

import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.dto.WishlistAddRequest;
import mexa.club.shopservice.dto.WishlistEntryResponse;
import mexa.club.shopservice.entity.WishlistItem;
import mexa.club.shopservice.repository.WishlistItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductCatalogService productCatalogService;

    public WishlistService(WishlistItemRepository wishlistItemRepository, ProductCatalogService productCatalogService) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productCatalogService = productCatalogService;
    }

    @Transactional(readOnly = true)
    public List<WishlistEntryResponse> list(UUID userId) {
        List<WishlistItem> rows = wishlistItemRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, ProductResponse> products = fetchProducts(rows);
        return rows.stream()
                .map(w -> toEnriched(w, products))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<WishlistEntryResponse> listPage(UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WishlistItem> rows = wishlistItemRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        Map<UUID, ProductResponse> products = fetchProducts(rows.getContent());
        return rows.map(w -> toEnriched(w, products));
    }

    @Transactional
    public WishlistEntryResponse add(UUID userId, WishlistAddRequest request) {
        ProductResponse product = productCatalogService.getProductById(request.productId());
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, request.productId())) {
            var existing = wishlistItemRepository.findByUserIdAndProductId(userId, request.productId());
            return existing.map(w -> toEnriched(w, product))
                    .orElse(toEnriched(request.productId(), null, product));
        }
        try {
            WishlistItem row = new WishlistItem();
            row.setUserId(userId);
            row.setProductId(request.productId());
            WishlistItem saved = wishlistItemRepository.save(row);
            return toEnriched(saved, product);
        } catch (DataIntegrityViolationException e) {
            var existing = wishlistItemRepository.findByUserIdAndProductId(userId, request.productId());
            return existing.map(w -> toEnriched(w, product))
                    .orElse(toEnriched(request.productId(), null, product));
        }
    }

    /** Mahsulot ma'lumotlari bilan boyitish (o'chirilgan mahsulotda null'lar). */
    private Map<UUID, ProductResponse> fetchProducts(List<WishlistItem> rows) {
        List<UUID> ids = rows.stream().map(WishlistItem::getProductId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            return productCatalogService.getProductsByIds(ids).stream()
                    .collect(Collectors.toMap(ProductResponse::id, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private WishlistEntryResponse toEnriched(WishlistItem w, Map<UUID, ProductResponse> products) {
        return toEnriched(w.getProductId(), w.getCreatedAt(), products.get(w.getProductId()));
    }

    private WishlistEntryResponse toEnriched(WishlistItem w, ProductResponse product) {
        return toEnriched(w.getProductId(), w.getCreatedAt(), product);
    }

    private WishlistEntryResponse toEnriched(UUID productId, java.time.Instant addedAt, ProductResponse product) {
        if (product == null) {
            return WishlistEntryResponse.bare(productId, addedAt);
        }
        String image = (product.imageUrls() != null && !product.imageUrls().isEmpty())
                ? product.imageUrls().get(0) : null;
        return new WishlistEntryResponse(productId, addedAt, product.name(), image);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID userId, UUID productId) {
        return wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
