package mexa.club.shopservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.shopservice.client.payload.PagePayload;
import mexa.club.shopservice.dto.ProductResponse;
import mexa.club.shopservice.service.ProductCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "Products", description = "Public product catalog endpoints for browsing and searching products")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /**
     * Mahsulotlar ro'yxati (keyset pagination).
     * GET /api/products?search=...&cursor=<uuid>&size=20
     * cursor berilmasa eski page-based rejim ishlaydi (backward compatibility).
     */
    @Operation(
        summary = "List products",
        description = "Returns a paginated list of products. Optionally filter by search keyword, minimum and maximum price, and currency. Use 'cursor' (UUID of the last item) for keyset pagination."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved product list"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    @GetMapping
    public PagePayload<ProductResponse> getProducts(
            @Parameter(description = "Keyword to search by product name or description")
            @RequestParam(required = false)                  String search,
            @Parameter(description = "Filter by category UUID")
            @RequestParam(required = false)                  UUID categoryId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false)                  BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false)                  BigDecimal maxPrice,
            @Parameter(description = "Currency code (e.g. UZS, USD)")
            @RequestParam(required = false)                  String currency,
            @Parameter(description = "Sort field and direction (e.g. price_asc, price_desc, name_asc)")
            @RequestParam(required = false)                  String sort,
            @Parameter(description = "Keyset cursor — oldingi sahifadagi oxirgi mahsulotning UUID'si (null = 1-sahifa)")
            @RequestParam(required = false)                  UUID cursor,
            @Parameter(description = "Zero-based page index (legacy, cursor ishlatilganda e'tiborsiz)")
            @RequestParam(defaultValue = "0")                int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20")               int size
    ) {
        if (cursor != null) {
            return productCatalogService.getProductsPage(search, categoryId, sort, cursor, size);
        }
        return productCatalogService.getProductsPage(search, categoryId, sort, page, size);
    }

    /**
     * Qidiruv (paginatsiyali).
     * GET /api/products/search?query=...&page=0&size=20
     */
    @Operation(
        summary = "Search products",
        description = "Search products by a text query. Accepts either 'query' or 'q' as the search parameter. Returns an empty page if no query is provided."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    @GetMapping("/search")
    public PagePayload<ProductResponse> searchProducts(
            @Parameter(description = "Search query string (alternative to 'q')")
            @RequestParam(required = false)    String query,
            @Parameter(description = "Search query string (short alias for 'query')")
            @RequestParam(required = false, name = "q") String q,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        String searchText = (query != null && !query.isBlank()) ? query : q;
        if (searchText == null || searchText.isBlank()) {
            return new PagePayload<>(java.util.List.of(), 0, 0, page, size);
        }
        return productCatalogService.getProductsPage(searchText, null, null, page, size);
    }

    @Operation(
        summary = "Get product by ID",
        description = "Returns the details of a single product identified by its UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ProductResponse getProductById(
            @Parameter(description = "UUID of the product", required = true)
            @PathVariable UUID id) {
        return productCatalogService.getProductById(id);
    }
}
