package mexa.club.shopservice.client;

import mexa.club.shopservice.client.payload.ApiResponse;
import mexa.club.shopservice.client.payload.PagePayload;
import mexa.club.shopservice.config.ProductServiceProperties;
import mexa.club.shopservice.dto.HomeBrandResponse;
import mexa.club.shopservice.dto.HomeCategoryResponse;
import mexa.club.shopservice.dto.PriceResolveResponse;
import mexa.club.shopservice.dto.ProductResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
public class ProductApiClient {

    private static final ParameterizedTypeReference<ApiResponse<PagePayload<ProductResponse>>> PAGE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<ProductResponse>> SINGLE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<List<ProductResponse>>> BATCH_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<PriceResolveResponse>> PRICE_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<PagePayload<HomeBrandResponse>>> BRAND_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<PagePayload<HomeCategoryResponse>>> CATEGORY_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient productRestClient;
    private final ProductServiceProperties productServiceProperties;

    public ProductApiClient(RestClient productRestClient, ProductServiceProperties productServiceProperties) {
        this.productRestClient = productRestClient;
        this.productServiceProperties = productServiceProperties;
    }

    /** Eski metod — backward compatibility uchun (page=0, defaultPageSize) */
    public List<ProductResponse> fetchProducts(String search, String bearerToken) {
        return fetchProductsPage(search, null, 0, productServiceProperties.defaultPageSize(), bearerToken).content();
    }

    /** Paginatsiyali mahsulotlar ro'yxati */
    public PagePayload<ProductResponse> fetchProductsPage(String search, UUID categoryId, int page, int size, String bearerToken) {
        ApiResponse<PagePayload<ProductResponse>> response = productRestClient.get()
                .uri(productsUri(search, categoryId, page, size))
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(PAGE_RESPONSE_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected response from product-service list endpoint");
        }
        return response.data();
    }

    /**
     * Keyset (cursor) pagination: product-service'ga afterId + active=true yuboriladi.
     * Filterlar DB tomonidan qo'llanilgani uchun hasMore hisobi aniq bo'ladi.
     */
    public PagePayload<ProductResponse> fetchProductsAfter(String search, UUID categoryId, UUID afterId, int size, String bearerToken) {
        ApiResponse<PagePayload<ProductResponse>> response = productRestClient.get()
                .uri(productsAfterUri(search, categoryId, afterId, size))
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(PAGE_RESPONSE_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected response from product-service list endpoint");
        }
        return response.data();
    }

    /** Eski metod — lang'siz (backward compatibility). */
    public ProductResponse fetchProductById(UUID id, String bearerToken) {
        return fetchProductById(id, null, bearerToken);
    }

    /** Mahsulotni berilgan tilda olish (lang=uz|ru — lokalizatsiyalangan javob). */
    public ProductResponse fetchProductById(UUID id, String lang, String bearerToken) {
        ApiResponse<ProductResponse> response = productRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/products/{id}")
                        .queryParamIfPresent("lang", Optional.ofNullable(lang).filter(StringUtils::hasText))
                        .build(id))
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(SINGLE_RESPONSE_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected response from product-service get-by-id endpoint");
        }
        return response.data();
    }

    /** Eski metod — lang'siz (backward compatibility). */
    public List<ProductResponse> fetchProductsByIds(List<UUID> ids, String bearerToken) {
        return fetchProductsByIds(ids, null, bearerToken);
    }

    /** BATCH: bir nechta mahsulotni bitta so'rovda, berilgan tilda olish (N+1 o'rniga). */
    public List<ProductResponse> fetchProductsByIds(List<UUID> ids, String lang, String bearerToken) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        ApiResponse<List<ProductResponse>> response = productRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/products/batch")
                        .queryParam("ids", String.join(",", ids.stream().map(UUID::toString).toList()))
                        .queryParamIfPresent("lang", Optional.ofNullable(lang).filter(StringUtils::hasText))
                        .build())
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(BATCH_RESPONSE_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected response from product-service batch endpoint");
        }
        return response.data();
    }

    /** Miqdorga bog'liq yagona narx: liniyalar (tiers) yoki muddatli chegirma bilan resolve. */
    public PriceResolveResponse fetchPrice(UUID productId, int qty, String bearerToken) {
        ApiResponse<PriceResolveResponse> response = productRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/products/{id}/price")
                        .queryParam("qty", Math.max(1, qty))
                        .build(productId))
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(PRICE_RESPONSE_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected response from product-service price resolve endpoint");
        }
        return response.data();
    }

    /** Homepage bundle uchun: faol brendlar (product-service'dan). */
    public List<HomeBrandResponse> fetchBrands(String bearerToken) {
        ApiResponse<PagePayload<HomeBrandResponse>> response = productRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/brands")
                        .queryParam("active", true)
                        .queryParam("size", 8)
                        .build())
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(BRAND_PAGE_TYPE);

        if (response == null || !response.success() || response.data() == null || response.data().content() == null) {
            throw new IllegalStateException("Unexpected response from product-service brands endpoint");
        }
        return response.data().content();
    }

    /** Homepage bundle uchun: faol kategoriyalar (product-service'dan). */
    public List<HomeCategoryResponse> fetchCategories(String bearerToken) {
        ApiResponse<PagePayload<HomeCategoryResponse>> response = productRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/categories")
                        .queryParam("active", true)
                        .queryParam("size", 100)
                        .build())
                .headers(headers -> addBearerToken(headers, bearerToken))
                .retrieve()
                .body(CATEGORY_PAGE_TYPE);

        if (response == null || !response.success() || response.data() == null || response.data().content() == null) {
            throw new IllegalStateException("Unexpected response from product-service categories endpoint");
        }
        return response.data().content();
    }

    private Function<UriBuilder, java.net.URI> productsUri(String search, UUID categoryId, int page, int size) {
        return uriBuilder -> uriBuilder.path("/api/products")
                .queryParamIfPresent("name", Optional.ofNullable(search).filter(StringUtils::hasText))
                .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                .queryParam("page", page)
                .queryParam("size", size)
                .build();
    }

    private Function<UriBuilder, java.net.URI> productsAfterUri(String search, UUID categoryId, UUID afterId, int size) {
        return uriBuilder -> uriBuilder.path("/api/products")
                .queryParamIfPresent("name", Optional.ofNullable(search).filter(StringUtils::hasText))
                .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                .queryParamIfPresent("afterId", Optional.ofNullable(afterId))
                .queryParam("page", 0)
                .queryParam("size", size)
                .queryParam("active", true)
                .build();
    }

    private static void addBearerToken(HttpHeaders headers, String bearerToken) {
        if (StringUtils.hasText(bearerToken)) {
            headers.set(HttpHeaders.AUTHORIZATION, bearerToken.trim());
        }
    }
}
