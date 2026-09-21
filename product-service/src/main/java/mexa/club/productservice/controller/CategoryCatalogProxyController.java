package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.productservice.api.ApiResponse;
import mexa.club.productservice.api.PagePayload;
import mexa.club.productservice.dto.CategoryRequest;
import mexa.club.productservice.dto.CategoryResponse;
import mexa.club.productservice.service.CategoryService;
import mexa.club.productservice.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Product category management: list, retrieve, create, update, delete categories, and manage category images. Supports hierarchical (parent/child) category structure.")
@SecurityRequirement(name = "bearerAuth")
public class CategoryCatalogProxyController {

    private final CategoryService categoryService;
    private final StorageService storageService;

    public CategoryCatalogProxyController(CategoryService categoryService, StorageService storageService) {
        this.categoryService = categoryService;
        this.storageService = storageService;
    }

    @GetMapping
    @Operation(
        summary = "List categories",
        description = "Returns a paginated list of all categories. Optionally filtered by name (partial match) and/or active status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated category list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<ApiResponse<PagePayload<CategoryResponse>>> list(
            @Parameter(description = "Filter by category name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<CategoryResponse> page = categoryService.list(name, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }

    @GetMapping("/roots")
    @Operation(
        summary = "List root categories",
        description = "Returns all top-level categories that have no parent. Use this to build category trees."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Root category list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> roots() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listRoots()));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get category by ID",
        description = "Returns a single category by its UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> get(
            @Parameter(description = "UUID of the category", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.get(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Create a category",
        description = "Creates a new product category. Set the parentId field in the request body to create a subcategory under an existing category."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category data to create", required = true)
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update a category",
        description = "Replaces all fields of an existing category with the provided JSON data."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @Parameter(description = "UUID of the category to update", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated category data", required = true)
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, request)));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Upload category image",
        description = "Uploads an image file for the given category. The uploaded image replaces any previously set image URL on the category."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image uploaded and updated category returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — file is missing or has an unsupported format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> uploadImage(
            @Parameter(description = "UUID of the category", required = true) @PathVariable UUID id,
            @Parameter(description = "Image file to upload", required = true) @RequestParam("file") MultipartFile file
    ) throws IOException {
        String relativePath = storageService.saveCategoryImage(id, file);
        String imageUrl = storageService.toPublicUrl(relativePath);
        CategoryResponse updated = categoryService.updateImage(id, imageUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete category image",
        description = "Removes the image from the specified category, setting the image URL to null."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image removed and updated category returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> deleteImage(
            @Parameter(description = "UUID of the category", required = true) @PathVariable UUID id) throws IOException {
        CategoryResponse updated = categoryService.updateImage(id, null);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a category",
        description = "Permanently deletes the category with the given ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Category deleted successfully — no content returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the category to delete", required = true) @PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
