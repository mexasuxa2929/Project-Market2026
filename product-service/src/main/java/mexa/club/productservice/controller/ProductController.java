package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.productservice.api.PagePayload;
import mexa.club.productservice.dto.ProductFullResponse;
import mexa.club.productservice.dto.ProductImportResult;
import mexa.club.productservice.dto.ProductActiveRequest;
import mexa.club.productservice.dto.ProductRequest;
import mexa.club.productservice.dto.ProductResponse;
import mexa.club.productservice.dto.PriceResolveResponse;
import mexa.club.productservice.dto.StockSummaryResponse;
import mexa.club.productservice.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog management: create, update, delete, search, filter, manage images, color variants, and export/import operations.")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(
        summary = "List products with filters",
        description = "Returns a paginated list of products. Supports filtering by name, barcode, category, brand, manufacturer, active status, tag, status, and featured flag."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated product list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<PagePayload<ProductResponse>>> listProducts(
            @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @Parameter(description = "Keyset cursor — faqat shu id'dan katta mahsulotlar qaytariladi (id ASC tartibda)") @RequestParam(required = false) UUID afterId,
            @Parameter(description = "Filter by product name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by exact barcode") @RequestParam(required = false) String barcode,
            @Parameter(description = "Filter by category UUID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Filter by brand UUID") @RequestParam(required = false) String brandId,
            @Parameter(description = "Filter by manufacturer UUID") @RequestParam(required = false) String manufacturerId,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by product tag") @RequestParam(required = false) String tag,
            @Parameter(description = "Filter by product status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by featured flag") @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Til kodi (uz|ru) — berilsa: lokalizatsiyalangan va yengil (slim) payload qaytariladi (mobile)") @RequestParam(required = false) String lang
    ) {
        var page = productService.findProducts(pageable, afterId, name, barcode, categoryId, brandId, manufacturerId, active, tag, status, featured, lang);
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(PagePayload.of(page)));
    }

    @GetMapping("/batch")
    @Operation(
        summary = "Get multiple products by IDs",
        description = "Returns products for the given comma-separated UUID list in a single request (cart/order services uchun N+1 oldini olish)."
    )
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<List<ProductResponse>>> getProductsByIds(
            @Parameter(description = "Comma-separated product UUIDs") @RequestParam List<UUID> ids,
            @Parameter(description = "Til kodi (uz|ru) — ixtiyoriy") @RequestParam(required = false) String lang) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.getProductsByIds(ids, lang)));
    }

    @GetMapping("/{id}/price")
    @Operation(
        summary = "Resolve unit price for a quantity",
        description = "Miqdorga bog'liq yagona narx: 2+ liniya bo'lsa mos liniya narxi (muddatli chegirma ishlamaydi), " +
                "aks holda asosiy narx ustidan muddatli chegirma qo'llanadi. Cart/buyurtma xizmatlari shu endpoint'dan foydalanadi."
    )
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<PriceResolveResponse>> resolvePrice(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @Parameter(description = "Miqdor (dona), default 1", example = "6") @RequestParam(defaultValue = "1") int qty
    ) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.resolvePrice(id, qty)));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get product by ID",
        description = "Returns a single product by its UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @Parameter(description = "Til kodi (uz|ru) — berilsa javob shu tilda lokalizatsiyalanadi") @RequestParam(required = false) String lang) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.getProductById(id, lang)));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search products (legacy)",
        description = "Legacy search endpoint — behaves identically to GET /api/products but requires at least one filter parameter to be present."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — no filter parameters provided"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<PagePayload<ProductResponse>>> searchLegacy(
            @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @Parameter(description = "Filter by product name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by exact barcode") @RequestParam(required = false) String barcode,
            @Parameter(description = "Filter by category UUID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Filter by brand UUID") @RequestParam(required = false) String brandId,
            @Parameter(description = "Filter by manufacturer UUID") @RequestParam(required = false) String manufacturerId,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by product tag") @RequestParam(required = false) String tag,
            @Parameter(description = "Filter by product status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by featured flag") @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Til kodi (uz|ru) — berilsa lokalizatsiyalangan yengil payload") @RequestParam(required = false) String lang
    ) {
        if ((name == null || name.isBlank())
                && (barcode == null || barcode.isBlank())
                && (categoryId == null || categoryId.isBlank())
                && (brandId == null || brandId.isBlank())
                && (manufacturerId == null || manufacturerId.isBlank())
                && active == null
                && (tag == null || tag.isBlank())
                && (status == null || status.isBlank())
                && featured == null) {
            return ResponseEntity.badRequest()
                    .body(mexa.club.productservice.api.ApiResponse.fail("BAD_REQUEST",
                            "Provide at least one filter: name, barcode, categoryId, brandId, manufacturerId, active, tag, status, featured"));
        }
        var page = productService.findProducts(pageable, null, name, barcode, categoryId, brandId, manufacturerId, active, tag, status, featured, lang);
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(PagePayload.of(page)));
    }

    @GetMapping("/tags")
    @Operation(
        summary = "List all product tags",
        description = "Returns the distinct list of all tags that are currently assigned to at least one product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tag list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<List<String>>> listTags() {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.listTags()));
    }

    @GetMapping("/by-barcode/{barcode}")
    @Operation(
        summary = "Get product by barcode",
        description = "Looks up a product by its unique barcode string."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No product found with the given barcode")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> getByBarcode(
            @Parameter(description = "Barcode value to look up", required = true) @PathVariable String barcode) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.getByBarcode(barcode)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Create product (JSON)",
        description = "Creates a new product from a JSON body. Use the multipart variant to upload product images in the same request."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> createProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Product data to create", required = true)
            @Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mexa.club.productservice.api.ApiResponse.ok(created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Create product with images (multipart)",
        description = "Creates a new product and simultaneously uploads one or more product images. Send the JSON product data as the 'product' part and image files as the 'files' part."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully with images"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the product part"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> createProductMultipart(
            @Parameter(description = "Product data as JSON part named 'product'", required = true)
            @Valid @RequestPart("product") ProductRequest request,
            @Parameter(description = "Optional image files to attach to the product")
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        ProductResponse created = productService.createProduct(request, normalizeFiles(files));
        return ResponseEntity.status(HttpStatus.CREATED).body(mexa.club.productservice.api.ApiResponse.ok(created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update product (JSON)",
        description = "Replaces all fields of an existing product with the data provided in the JSON body."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "UUID of the product to update", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated product data", required = true)
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.updateProduct(id, request)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update product with images (multipart)",
        description = "Updates an existing product and optionally replaces or adds images. Send the JSON product data as the 'product' part and new image files as the 'files' part."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully with images"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the product part"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> updateProductMultipart(
            @Parameter(description = "UUID of the product to update", required = true) @PathVariable UUID id,
            @Parameter(description = "Updated product data as JSON part named 'product'", required = true)
            @Valid @RequestPart("product") ProductRequest request,
            @Parameter(description = "Optional new image files to attach")
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.updateProduct(id, request, normalizeFiles(files))));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Upload additional images to a product",
        description = "Uploads one or more image files and appends them to the existing image list of the product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images uploaded and product returned with updated image list"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — no files provided"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> uploadProductImages(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @Parameter(description = "Image files to upload (at least one required)")
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        List<MultipartFile> list = normalizeFiles(files);
        if (list.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(mexa.club.productservice.api.ApiResponse.fail("BAD_REQUEST", "Provide at least one file in 'files'"));
        }
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.addProductImages(id, list)));
    }

    @GetMapping("/{id}/images")
    @Operation(
        summary = "List product images",
        description = "Returns the ordered list of public image URLs for the given product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image URL list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<List<String>>> listProductImages(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.listProductImages(id)));
    }

    @DeleteMapping("/{id}/images/{filename:.+}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a product image",
        description = "Removes a specific image file from the product and returns the updated product."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted and updated product returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or image not found")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> deleteProductImage(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @Parameter(description = "Filename of the image to delete", required = true) @PathVariable String filename
    ) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.removeProductImage(id, filename)));
    }

    @PutMapping("/{id}/images/reorder")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Reorder product images",
        description = "Sets the display order of product images. Send a JSON body with a 'urls' array containing the image URLs in the desired order."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images reordered and updated product returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> reorderProductImages(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON object with 'urls' array listing image URLs in the desired order", required = true)
            @RequestBody java.util.Map<String, java.util.List<String>> body
    ) {
        java.util.List<String> urls = body.getOrDefault("urls", java.util.List.of());
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.reorderProductImages(id, urls)));
    }

    private static List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    @PatchMapping("/{id}/active")
    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Toggle product active status",
        description = "Sets the active/inactive flag on a product. Accepts an optional JSON body with an 'active' boolean field. If the body is omitted, the active status is toggled."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product active status updated and product returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> patchActive(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional body with 'active' boolean field")
            @RequestBody(required = false) ProductActiveRequest body
    ) {
        Boolean active = body != null ? body.getActive() : null;
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.setProductActive(id, active)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a product",
        description = "Permanently deletes the product with the given ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Product deleted successfully — no content returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "UUID of the product to delete", required = true) @PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bir xil mahsulotning boshqa rangdagi nusxalarini (bir guruhdagi barchasini) qaytaradi.
     * Variantlar olib tashlandi — endi har rang mustaqil Product, "group_id" orqali bog'langan.
     */
    @GetMapping("/{id}/colors")
    @Operation(
        summary = "List color siblings of a product",
        description = "Returns all products that share the same color group (group_id) as the specified product. Each color variant is stored as an independent product with its own barcode and pricing."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Color sibling list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<List<ProductResponse>>> listColorGroup(
            @Parameter(description = "UUID of the product whose color siblings to retrieve", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.listColorGroup(id)));
    }

    /** Desktop app uchun yengil endpoint: joriy mahsulot va uning rang guruhidoshlarini qaytaradi. */
    @GetMapping("/{id}/siblings")
    @Operation(summary = "List color siblings (lightweight)", description = "Returns a lightweight list of color siblings for the given product, including the product itself marked as isCurrent=true.")
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<List<mexa.club.productservice.dto.ProductColorSiblingResponse>>> listColorSiblings(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.listColorSiblings(id)));
    }

    /**
     * Mavjud mahsulotga yangi rang (mustaqil Product, bir xil group_id, o'z barcode/narxi bilan) qo'shadi.
     */
    @PostMapping(value = "/{id}/colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Add a color variant to a product",
        description = "Creates a new independent product that belongs to the same color group as the specified product. The new product has its own barcode, pricing, and images, but is linked via group_id."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Color variant product created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Source product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductResponse>> createColorSibling(
            @Parameter(description = "UUID of the existing product to add a color variant to", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Product data for the new color variant", required = true)
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mexa.club.productservice.api.ApiResponse.ok(productService.createColorSibling(id, request)));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Import products from a file",
        description = "Bulk-imports products from an uploaded Excel or CSV file. Returns an import result summary including the number of created, updated, and failed rows."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Import completed — result summary returned (may include partial failures)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — file is missing or has an unsupported format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductImportResult>> importProducts(
            @Parameter(description = "Excel (.xlsx) or CSV file containing product rows", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.importProducts(file)));
    }

    @GetMapping("/export")
    @Operation(
        summary = "Export products to Excel",
        description = "Exports the product catalog to an Excel file. Optionally filtered by category and/or active status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Excel file returned as a binary response"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<byte[]> exportProducts(
            @Parameter(description = "Filter exported products by category UUID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Filter exported products by active status") @RequestParam(required = false) Boolean active
    ) {
        return productService.exportProducts(categoryId, active);
    }

    @GetMapping("/{id}/stock-summary")
    @Operation(
        summary = "Get stock summary for a product",
        description = "Returns the aggregated warehouse stock information for the specified product, including total quantity on hand."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock summary returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<StockSummaryResponse>> stockSummary(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.stockSummary(id)));
    }

    /**
     * Shop-facing: returns full product enriched with total stock and sibling colors
     * (other Products in the same group_id).
     */
    @GetMapping("/{id}/full")
    @Operation(
        summary = "Get full product details",
        description = "Returns a richly populated product view including pricing details, total stock on hand, and the list of color sibling products that share the same group_id. Intended for shop-facing product detail pages."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Full product details returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found for the given ID")
    })
    public ResponseEntity<mexa.club.productservice.api.ApiResponse<ProductFullResponse>> getProductFull(
            @Parameter(description = "UUID of the product", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(mexa.club.productservice.api.ApiResponse.ok(productService.getProductFull(id)));
    }
}
