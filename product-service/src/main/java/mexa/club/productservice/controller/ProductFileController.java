package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.productservice.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Optional;
import java.util.UUID;

/**
 * Diskdan rasmlarni xavfsiz berish — faqat {@code images/products/...} ostidagi fayllar.
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "File Serving", description = "Public endpoints for serving stored image files for products, brands, and categories. No authentication required.")
public class ProductFileController {

    private final StorageService storageService;

    public ProductFileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/images/products/{productId}/{filename:.+}")
    @Operation(
        summary = "Serve a product image",
        description = "Returns the binary image file for a product. The response Content-Type is inferred from the filename extension. Cached by the browser for 24 hours."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image file returned successfully"),
        @ApiResponse(responseCode = "404", description = "Image file not found for the given product ID and filename")
    })
    public ResponseEntity<Resource> getProductImage(
            @Parameter(description = "UUID of the product that owns the image", required = true) @PathVariable UUID productId,
            @Parameter(description = "Filename of the image including extension (e.g. photo.jpg)", required = true) @PathVariable String filename
    ) throws IOException {
        Optional<Resource> resourceOpt = storageService.loadAsResource(productId, filename);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = resourceOpt.get();
        String contentType = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/images/brands/{brandId}/{filename:.+}")
    @Operation(
        summary = "Serve a brand image",
        description = "Returns the binary image file for a brand logo. The response Content-Type is inferred from the filename extension. Cached by the browser for 24 hours."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Brand image file returned successfully"),
        @ApiResponse(responseCode = "404", description = "Image file not found for the given brand ID and filename")
    })
    public ResponseEntity<Resource> getBrandImage(
            @Parameter(description = "UUID of the brand that owns the image", required = true) @PathVariable UUID brandId,
            @Parameter(description = "Filename of the image including extension (e.g. logo.png)", required = true) @PathVariable String filename
    ) throws IOException {
        Optional<Resource> resourceOpt = storageService.loadBrandAsResource(brandId, filename);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = resourceOpt.get();
        String contentType = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/images/categories/{categoryId}/{filename:.+}")
    @Operation(
        summary = "Serve a category image",
        description = "Returns the binary image file for a product category. The response Content-Type is inferred from the filename extension. Cached by the browser for 24 hours."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category image file returned successfully"),
        @ApiResponse(responseCode = "404", description = "Image file not found for the given category ID and filename")
    })
    public ResponseEntity<Resource> getCategoryImage(
            @Parameter(description = "UUID of the category that owns the image", required = true) @PathVariable UUID categoryId,
            @Parameter(description = "Filename of the image including extension (e.g. banner.webp)", required = true) @PathVariable String filename
    ) throws IOException {
        Optional<Resource> resourceOpt = storageService.loadCategoryAsResource(categoryId, filename);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = resourceOpt.get();
        String contentType = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }
}
