package mexa.club.shopservice.service;

import mexa.club.shopservice.client.ProductApiClient;
import mexa.club.shopservice.client.payload.PagePayload;
import mexa.club.shopservice.dto.HomeBrandResponse;
import mexa.club.shopservice.dto.HomeCategoryResponse;
import mexa.club.shopservice.dto.HomeResponse;
import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.dto.RecommendedProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Bosh sahifa bundle: trending + recommended + brands + categories bitta javobda.
 * 4 ta manba parallel yuklanadi — mobil app 4 round-trip o'rniga 1 ta qiladi.
 *
 * Muhim: auth header so'rov thread'ida olinib, async thread'larga authHeader
 * parametri orqali uzatiladi (RequestContextHolder worker thread'larda ishlamaydi).
 * Har bir manba mustaqil: biri xato bersa qolganlari baribir qaytadi.
 */
@Service
public class HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);

    private static final int TRENDING_SIZE = 20;
    private static final int RECOMMENDED_LIMIT = 10;
    private static final int SOURCE_TIMEOUT_SECONDS = 10;

    private final ProductCatalogService productCatalogService;
    private final ProductReviewService productReviewService;
    private final ProductApiClient productApiClient;

    /** Bloklovchi HTTP chaqiriqlar uchun alohida pool (common pool'ni band qilmaslik uchun). */
    private final Executor homeExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "home-bundle-");
        t.setDaemon(true);
        return t;
    });

    public HomeService(ProductCatalogService productCatalogService,
                       ProductReviewService productReviewService,
                       ProductApiClient productApiClient) {
        this.productCatalogService = productCatalogService;
        this.productReviewService = productReviewService;
        this.productApiClient = productApiClient;
    }

    public HomeResponse getHome(String authHeader) {
        // Request attributelarini worker thread'larga uzatish
        // (RequestTokenProvider ichki servislarda shu orqali token oladi)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<PagePayload<ProductResponse>> trendingFuture = supplyAsyncWithContext(requestAttributes,
                () -> productCatalogService.getProductsPage(null, null, null, 0, TRENDING_SIZE));

        CompletableFuture<List<RecommendedProductResponse>> recommendedFuture = supplyAsyncWithContext(requestAttributes,
                () -> productReviewService.getRecommended(RECOMMENDED_LIMIT, true));

        CompletableFuture<List<HomeBrandResponse>> brandsFuture = supplyAsyncWithContext(requestAttributes,
                () -> productApiClient.fetchBrands(authHeader));

        CompletableFuture<List<HomeCategoryResponse>> categoriesFuture = supplyAsyncWithContext(requestAttributes,
                () -> productApiClient.fetchCategories(authHeader));

        PagePayload<ProductResponse> trending = joinOrDefault(trendingFuture,
                new PagePayload<>(List.of(), 0, 0, 0, TRENDING_SIZE), "trending");
        List<RecommendedProductResponse> recommended = joinOrDefault(recommendedFuture, List.of(), "recommended");
        List<HomeBrandResponse> brands = joinOrDefault(brandsFuture, List.of(), "brands");
        List<HomeCategoryResponse> categories = joinOrDefault(categoriesFuture, List.of(), "categories");

        List<ProductResponse> trendingList = trending.content() != null ? trending.content() : List.of();
        return new HomeResponse(
                trendingList,
                trending.hasMore(),
                trending.nextCursor(),
                recommended,
                brands,
                categories
        );
    }

    private <T> CompletableFuture<T> supplyAsyncWithContext(RequestAttributes attributes, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (attributes != null) {
                RequestContextHolder.setRequestAttributes(attributes);
            }
            try {
                return supplier.get();
            } finally {
                if (attributes != null) {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        }, homeExecutor);
    }

    private <T> T joinOrDefault(CompletableFuture<T> future, T defaultValue, String source) {
        try {
            T value = future.orTimeout(SOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.warn("home/{} source failed, returning default: {}", source, e.getMessage());
            return defaultValue;
        }
    }
}
