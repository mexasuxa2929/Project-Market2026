package mexa.club.shopservice.service;

import mexa.club.shopservice.dto.CartLineResponse;
import mexa.club.shopservice.dto.CartResponse;
import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.entity.CartItem;
import mexa.club.shopservice.entity.ShopCart;
import mexa.club.shopservice.repository.CartItemRepository;
import mexa.club.shopservice.repository.ShopCartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final ShopCartRepository shopCartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductCatalogService productCatalogService;

    public CartService(
            ShopCartRepository shopCartRepository,
            CartItemRepository cartItemRepository,
            ProductCatalogService productCatalogService
    ) {
        this.shopCartRepository = shopCartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productCatalogService = productCatalogService;
    }

    /** Eski metod — lang'siz (backward compatibility). */
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        return getCart(userId, null);
    }

    /** Savatni berilgan tilda olish — mahsulot nomlari backend'da lokalizatsiyalanadi. */
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId, String lang) {
        return shopCartRepository.findByUserId(userId)
                .map(cart -> {
                    List<CartItem> rows = cartItemRepository.findAllByCart_IdOrderByCreatedAtAsc(cart.getId());
                    List<ProductResponse> products = fetchProducts(rows, lang);
                    List<CartLineResponse> lines = rows.stream()
                            .map(row -> toLine(row, findProduct(products, row.getProductId())))
                            .toList();
                    int totalQty = rows.stream().mapToInt(CartItem::getQuantity).sum();
                    return new CartResponse(lines, totalQty, rows.size());
                })
                .orElseGet(() -> new CartResponse(List.of(), 0, 0));
    }

    /** BATCH: barcha cart satrlari uchun bitta HTTP so'rov (N+1 o'rniga). */
    private List<ProductResponse> fetchProducts(List<CartItem> rows, String lang) {
        List<UUID> ids = rows.stream().map(CartItem::getProductId).distinct().toList();
        return productCatalogService.getProductsByIds(ids, lang);
    }

    private static ProductResponse findProduct(List<ProductResponse> products, UUID productId) {
        return products.stream().filter(p -> p.id().equals(productId)).findFirst().orElse(null);
    }

    /** Eski metod — lang'siz (backward compatibility). */
    @Transactional
    public CartLineResponse upsertLine(UUID userId, UUID productId, int quantity) {
        return upsertLine(userId, productId, quantity, null);
    }

    /** Savat satrini berilgan tilda yangilash — javobdagi nom joriy tilda bo'ladi. */
    @Transactional
    public CartLineResponse upsertLine(UUID userId, UUID productId, int quantity, String lang) {
        ProductResponse product = productCatalogService.getProductById(productId, lang);
        ShopCart cart = getOrCreateCart(userId);
        CartItem row = cartItemRepository.findByCart_IdAndProductId(cart.getId(), productId).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setCart(cart);
            created.setProductId(productId);
            return created;
        });
        row.setQuantity(quantity);
        // Miqdorga bog'liq narx: liniyalar (tiers) yoki muddatli chegirma bo'yicha resolve qilinadi.
        // Narx topilmasa null saqlanadi — toLine null unitPrice qaytaradi (frontend eski qiymatni ishlatadi).
        row.setPrice(productCatalogService.resolveUnitPrice(productId, quantity));
        cart.setUpdatedAt(Instant.now());
        shopCartRepository.save(cart);
        return toLine(cartItemRepository.save(row), product);
    }

    @Transactional
    public void removeLine(UUID userId, UUID productId) {
        shopCartRepository.findByUserId(userId).ifPresent(cart ->
                cartItemRepository.deleteByCart_IdAndProductId(cart.getId(), productId));
    }

    @Transactional
    public void clear(UUID userId) {
        shopCartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteByCart_Id(cart.getId()));
    }

    private ShopCart getOrCreateCart(UUID userId) {
        return shopCartRepository.findByUserId(userId).orElseGet(() -> {
            ShopCart c = new ShopCart();
            c.setUserId(userId);
            return shopCartRepository.save(c);
        });
    }

    private CartLineResponse toLine(CartItem row, ProductResponse product) {
        String imageUrl = (product != null && product.imageUrls() != null && !product.imageUrls().isEmpty())
                ? product.imageUrls().get(0) : null;
        return new CartLineResponse(
                row.getProductId(), row.getQuantity(), row.getPrice(), row.getUpdatedAt(),
                product != null ? product.name() : null,
                imageUrl,
                product != null ? product.color() : null,
                product != null ? product.colorCode() : null
        );
    }
}
