package mexa.club.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.searchservice.client.ProductServiceClient;
import mexa.club.searchservice.dto.ApiResponse;
import mexa.club.searchservice.dto.ProductIndexRequest;
import mexa.club.searchservice.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "Internal - Search Index", description = "Internal endpoints for managing the Elasticsearch product index. Protected by X-Internal-Secret header.")
@RestController
@RequestMapping("/internal/search/products")
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class InternalSearchController {
    private final SearchService searchService;
    private final ProductServiceClient productServiceClient;
    private final String internalSecret;

    public InternalSearchController(SearchService searchService, ProductServiceClient productServiceClient,
                                    @Value("${app.internal-secret}") String internalSecret) {
        this.searchService = searchService;
        this.productServiceClient = productServiceClient;
        this.internalSecret = internalSecret;
    }

    @Operation(
        summary = "Index a single product",
        description = "Indexes or re-indexes a single product document in Elasticsearch. Called by product-service when a product is created or updated."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product indexed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product index request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PostMapping("/index")
    public ApiResponse<Map<String, Object>> index(
            @Parameter(description = "Internal service secret key", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestBody ProductIndexRequest request) {
        ensure(secret);
        searchService.index(request);
        return ApiResponse.ok(Map.of("indexed", true));
    }

    @Operation(
        summary = "Update a product in the index",
        description = "Updates an existing product document in Elasticsearch by its ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated in index successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
            @Parameter(description = "Internal service secret key", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Elasticsearch document ID of the product", required = true)
            @PathVariable String id,
            @RequestBody ProductIndexRequest request) {
        ensure(secret);
        searchService.index(new ProductIndexRequest(
                id, request.name(), request.barcode(), request.description(), request.categoryId(), request.categoryName(),
                request.brandId(), request.brandName(), request.tags(), request.salePrice(), request.active(),
                request.imageUrl(), request.inStock(), request.createdAt()
        ));
        return ApiResponse.ok(Map.of("updated", true));
    }

    @Operation(
        summary = "Delete a product from the index",
        description = "Removes the specified product document from Elasticsearch. Called when a product is deleted in product-service."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted from index successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found in index")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(
            @Parameter(description = "Internal service secret key", required = true)
            @RequestHeader("X-Internal-Secret") String secret,
            @Parameter(description = "Elasticsearch document ID of the product", required = true)
            @PathVariable String id) {
        ensure(secret);
        searchService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @Operation(
        summary = "Full reindex from product-service",
        description = "Fetches all products from product-service and re-indexes them all in Elasticsearch. Use with caution — this is a heavy operation."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reindex completed; response includes count of products reindexed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Invalid or missing X-Internal-Secret header")
    })
    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex(
            @Parameter(description = "Internal service secret key", required = true)
            @RequestHeader("X-Internal-Secret") String secret) {
        ensure(secret);
        List<Map<String, Object>> products = extractContent(productServiceClient.products(0, 1000).data());
        for (Map<String, Object> p : products) {
            searchService.index(mapProduct(p));
        }
        return ApiResponse.ok(Map.of("reindexedCount", products.size()));
    }

    private ProductIndexRequest mapProduct(Map<String, Object> p) {
        return new ProductIndexRequest(
                String.valueOf(p.get("id")),
                String.valueOf(p.get("name")),
                String.valueOf(p.getOrDefault("barcode", "")),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("categoryId", "")),
                String.valueOf(p.getOrDefault("categoryName", "")),
                String.valueOf(p.getOrDefault("brandId", "")),
                String.valueOf(p.getOrDefault("brandName", "")),
                List.of(),
                parseDouble(p.get("salePrice")),
                Boolean.TRUE.equals(p.get("active")),
                String.valueOf(p.getOrDefault("imageUrl", "")),
                true,
                LocalDateTime.now()
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Map<String, Object> map) {
        if (map == null) return List.of();
        Object content = map.get("content");
        if (content instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    private Double parseDouble(Object v) {
        if (v == null) return 0d;
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
    }

    private void ensure(String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new IllegalArgumentException("Invalid internal secret");
        }
    }
}
