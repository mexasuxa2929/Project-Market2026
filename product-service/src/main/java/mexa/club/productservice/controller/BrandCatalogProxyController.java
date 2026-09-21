package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.productservice.api.ApiResponse;
import mexa.club.productservice.api.PagePayload;
import mexa.club.productservice.dto.BrandRequest;
import mexa.club.productservice.dto.BrandResponse;
import mexa.club.productservice.service.BrandService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@Tag(name = "Brands", description = "Brand catalog management: list, retrieve, create, update, and delete product brands.")
@SecurityRequirement(name = "bearerAuth")
public class BrandCatalogProxyController {

    private final BrandService brandService;

    public BrandCatalogProxyController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @Operation(
        summary = "List brands",
        description = "Returns a paginated list of brands. Optionally filtered by name (partial match) and/or active status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated brand list returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority")
    })
    public ResponseEntity<ApiResponse<PagePayload<BrandResponse>>> list(
            @Parameter(description = "Filter by brand name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<BrandResponse> page = brandService.list(name, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get brand by ID",
        description = "Returns a single brand by its UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand found and returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found for the given ID")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> get(
            @Parameter(description = "UUID of the brand", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.get(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Create a brand",
        description = "Creates a new product brand from the provided JSON data."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Brand created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Brand data to create", required = true)
            @Valid @RequestBody BrandRequest request) {
        BrandResponse created = brandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update a brand",
        description = "Replaces all fields of an existing brand with the provided JSON data."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found for the given ID")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> update(
            @Parameter(description = "UUID of the brand to update", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated brand data", required = true)
            @Valid @RequestBody BrandRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a brand",
        description = "Permanently deletes the brand with the given ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Brand deleted successfully — no content returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found for the given ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the brand to delete", required = true) @PathVariable UUID id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
