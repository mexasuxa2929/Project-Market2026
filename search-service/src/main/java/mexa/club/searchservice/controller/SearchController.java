package mexa.club.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.searchservice.document.ProductSearchDocument;
import mexa.club.searchservice.dto.ApiResponse;
import mexa.club.searchservice.dto.SearchResponse;
import mexa.club.searchservice.service.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Search", description = "Elasticsearch-backed product search, autocomplete, and similar product discovery")
@RestController
@RequestMapping("/api/search")
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(
        summary = "Search products",
        description = "Full-text search across the product catalog with optional filters for category, brand, price range, stock availability, and tags. Results can be sorted by relevance, price, or other criteria."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @GetMapping("/products")
    public ApiResponse<SearchResponse> products(
            @Parameter(description = "Full-text search query")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filter by category name or ID")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by brand name or ID")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Filter to only in-stock products")
            @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "Filter by one or more product tags")
            @RequestParam(required = false) List<String> tags,
            @Parameter(description = "Sort order: relevance, price_asc, price_desc, newest")
            @RequestParam(defaultValue = "relevance") String sort,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of results per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(searchService.search(q, category, brand, minPrice, maxPrice, inStock, tags, sort, page, size));
    }

    @Operation(
        summary = "Autocomplete product names",
        description = "Returns a list of product name suggestions matching the given prefix query. Useful for implementing search-as-you-type functionality."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autocomplete suggestions returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Query parameter 'q' is required")
    })
    @GetMapping("/autocomplete")
    public ApiResponse<List<String>> autocomplete(
            @Parameter(description = "Prefix to autocomplete", required = true)
            @RequestParam String q,
            @Parameter(description = "Maximum number of suggestions to return")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.ok(searchService.autocomplete(q, limit));
    }

    @Operation(
        summary = "Find similar products",
        description = "Returns a list of products that are similar to the specified product, based on category, tags, and other attributes."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Similar products returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/products/{id}/similar")
    public ApiResponse<List<ProductSearchDocument>> similar(
            @Parameter(description = "ID of the product to find similar items for", required = true)
            @PathVariable String id,
            @Parameter(description = "Maximum number of similar products to return")
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(searchService.similar(id, limit));
    }
}
