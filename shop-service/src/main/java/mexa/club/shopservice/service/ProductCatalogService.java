package mexa.club.shopservice.service;

import mexa.club.shopservice.client.ProductApiClient;
import mexa.club.shopservice.client.payload.PagePayload;
import mexa.club.shopservice.dto.PriceResolveResponse;
import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.exception.ProductNotFoundException;
import mexa.club.shopservice.security.RequestTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProductCatalogService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductApiClient productApiClient;
    private final RequestTokenProvider requestTokenProvider;

    public ProductCatalogService(ProductApiClient productApiClient, RequestTokenProvider requestTokenProvider) {
        this.productApiClient = productApiClient;
        this.requestTokenProvider = requestTokenProvider;
    }

    public List<ProductResponse> getAllProducts(String search) {
        return getProductsPage(search, null, null, 0, 20).content();
    }

    public List<ProductResponse> getProducts(String search, BigDecimal minPrice, BigDecimal maxPrice, String currency) {
        return getProductsPage(search, null, null, 0, 20).content();
    }

    /** Paginatsiyali mahsulotlar ro'yxati — hasMore va nextCursor to'g'ri hisoblanadi */
    public PagePayload<ProductResponse> getProductsPage(String search, UUID categoryId, String sort, int page, int size) {
        String normalizedSearch = normalize(search);
        int limited = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));
        try {
            PagePayload<ProductResponse> raw = productApiClient.fetchProductsPage(
                    normalizedSearch, categoryId, page, limited + 1,
                    requestTokenProvider.resolveAuthorizationHeader()
            );
            List<ProductResponse> items = raw.content().stream()
                    .filter(ProductResponse::active)
                    .toList();
            boolean hasMore = items.size() > limited;
            List<ProductResponse> content = hasMore ? new ArrayList<>(items.subList(0, limited)) : new ArrayList<>(items);
            content = applySort(content, sort);
            UUID nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).id();
            int totalPages = (int) Math.ceil((double) raw.totalElements() / limited);
            return new PagePayload<>(content, raw.totalElements(), totalPages, page, limited, nextCursor, hasMore);
        } catch (RestClientException | IllegalStateException ex) {
            return new PagePayload<>(List.of(), 0, 0, page, limited);
        }
    }

    /**
     * Keyset (cursor) pagination: id tartibida, OFFSET'siz.
     * size+1 qator olinadi — hasMore shu orqali aniqlanadi, keyingi cursor = oxirgi qator id.
     */
    public PagePayload<ProductResponse> getProductsPage(String search, UUID categoryId, String sort, UUID cursor, int size) {
        String normalizedSearch = normalize(search);
        int limited = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));
        try {
            PagePayload<ProductResponse> raw = productApiClient.fetchProductsAfter(
                    normalizedSearch, categoryId, cursor, limited + 1,
                    requestTokenProvider.resolveAuthorizationHeader()
            );
            List<ProductResponse> items = raw.content().stream()
                    .filter(ProductResponse::active)
                    .toList();
            boolean hasMore = items.size() > limited;
            List<ProductResponse> page = hasMore ? new ArrayList<>(items.subList(0, limited)) : new ArrayList<>(items);
            page = applySort(page, sort);
            UUID nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).id();
            int totalPages = (int) Math.ceil((double) raw.totalElements() / limited);
            return new PagePayload<>(page, raw.totalElements(), totalPages, 0, limited, nextCursor, hasMore);
        } catch (RestClientException | IllegalStateException ex) {
            return new PagePayload<>(List.of(), 0, 0, 0, limited);
        }
    }

    /** Eski metod — lang'siz (backward compatibility). */
    public ProductResponse getProductById(UUID id) {
        return getProductById(id, null);
    }

    /** Mahsulotni berilgan tilda olish — nom/izohlar backend'da lokalizatsiyalanadi. */
    public ProductResponse getProductById(UUID id, String lang) {
        try {
            ProductResponse product = productApiClient.fetchProductById(id, lang, requestTokenProvider.resolveAuthorizationHeader());
            if (!product.active()) {
                throw new ProductNotFoundException(id);
            }
            return product;
        } catch (RestClientException | IllegalStateException ex) {
            throw new ProductNotFoundException(id);
        }
    }

    /** Eski metod — lang'siz (backward compatibility). */
    public List<ProductResponse> getProductsByIds(List<UUID> ids) {
        return getProductsByIds(ids, null);
    }

    /** BATCH: bir nechta mahsulotni bitta so'rovda, berilgan tilda olish (N+1 o'rniga). */
    public List<ProductResponse> getProductsByIds(List<UUID> ids, String lang) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            return productApiClient.fetchProductsByIds(ids, lang, requestTokenProvider.resolveAuthorizationHeader());
        } catch (RestClientException | IllegalStateException ex) {
            return List.of();
        }
    }

    public List<ProductResponse> searchProducts(String query) {
        return getProductsPage(query, null, null, 0, 20).content();
    }

    /**
     * Miqdorga bog'liq narx resolve: 2+ liniya bo'lsa liniya narxi (muddatli chegirma ishlamaydi),
     * aks holda chegirmali asosiy narx. Narx topilmasa null qaytadi.
     */
    public java.math.BigDecimal resolveUnitPrice(UUID productId, int qty) {
        try {
            PriceResolveResponse r = productApiClient.fetchPrice(
                    productId, qty, requestTokenProvider.resolveAuthorizationHeader());
            return r.unitPrice();
        } catch (RestClientException | IllegalStateException ex) {
            return null;
        }
    }

    private List<ProductResponse> applySort(List<ProductResponse> items, String sort) {
        if (sort == null || sort.isBlank()) return items;
        List<ProductResponse> sorted = new ArrayList<>(items);
        switch (sort) {
            case "price_asc"  -> sorted.sort(Comparator.comparing(p -> p.basePrice() != null ? p.basePrice() : BigDecimal.ZERO));
            case "price_desc" -> sorted.sort(Comparator.comparing((ProductResponse p) -> p.basePrice() != null ? p.basePrice() : BigDecimal.ZERO).reversed());
            case "name_asc"   -> sorted.sort(Comparator.comparing(p -> p.name() != null ? p.name() : ""));
            case "name_desc"  -> sorted.sort(Comparator.comparing((ProductResponse p) -> p.name() != null ? p.name() : "").reversed());
            case "newest"     -> sorted.sort(Comparator.comparing((ProductResponse p) -> p.createdAt() != null ? p.createdAt() : java.time.LocalDateTime.MIN).reversed());
            case "rating_desc"-> sorted.sort(Comparator.comparingDouble((ProductResponse p) -> p.avgRating() != null ? p.avgRating() : 0.0).reversed());
            case "popular"    -> sorted.sort(Comparator.comparingLong((ProductResponse p) -> p.reviewCount() != null ? p.reviewCount() : 0L).reversed());
        }
        return sorted;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
