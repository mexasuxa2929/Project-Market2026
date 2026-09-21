package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.productservice.entity.BrandLogo;
import mexa.club.productservice.repository.BrandLogoRepository;
import mexa.club.productservice.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands/{brandId}/logo")
@Tag(name = "Brand Logo", description = "Upload, retrieve, and delete the logo image for a brand.")
@SecurityRequirement(name = "bearerAuth")
public class BrandLogoController {

    private final StorageService storageService;
    private final BrandLogoRepository brandLogoRepository;

    public BrandLogoController(StorageService storageService,
                               BrandLogoRepository brandLogoRepository) {
        this.storageService = storageService;
        this.brandLogoRepository = brandLogoRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Upload or replace a brand logo",
        description = "Uploads an image file as the logo for the specified brand. If a logo already exists it is deleted from storage before the new file is saved. Returns the public URL of the uploaded logo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logo uploaded successfully — returns brandId and public logoUrl"),
        @ApiResponse(responseCode = "400", description = "Bad request — file is missing or has an unsupported format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @ApiResponse(responseCode = "404", description = "Brand not found for the given brandId")
    })
    public ResponseEntity<Map<String, Object>> upload(
            @Parameter(description = "UUID of the brand to upload the logo for", required = true) @PathVariable UUID brandId,
            @Parameter(description = "Logo image file (JPEG, PNG, WebP, etc.)", required = true) @RequestParam("file") MultipartFile file
    ) throws IOException {
        // Eski logoni o'chirish
        Optional<BrandLogo> existing = brandLogoRepository.findById(brandId);
        if (existing.isPresent()) {
            storageService.deleteRelativePath(existing.get().getPath());
        }

        String relativePath = storageService.saveBrand(brandId, file);

        BrandLogo logo = existing.orElse(new BrandLogo());
        logo.setBrandId(brandId);
        logo.setPath(relativePath);
        logo.setCreatedAt(Instant.now());
        brandLogoRepository.save(logo);

        String publicUrl = storageService.toPublicUrl(relativePath);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("brandId", brandId);
        data.put("logoUrl", publicUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(
        summary = "Get the logo for a brand",
        description = "Returns the public URL of the current logo for the specified brand."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logo found — returns brandId and public logoUrl"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @ApiResponse(responseCode = "404", description = "No logo found for the given brandId")
    })
    public ResponseEntity<Map<String, Object>> get(
            @Parameter(description = "UUID of the brand", required = true) @PathVariable UUID brandId) {
        Optional<BrandLogo> logo = brandLogoRepository.findById(brandId);
        if (logo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String publicUrl = storageService.toPublicUrl(logo.get().getPath());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("brandId", brandId);
        data.put("logoUrl", publicUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete the logo for a brand",
        description = "Removes the logo image file from storage and deletes the logo record for the specified brand."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logo deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @ApiResponse(responseCode = "404", description = "No logo found for the given brandId")
    })
    public ResponseEntity<Map<String, Object>> delete(
            @Parameter(description = "UUID of the brand whose logo should be deleted", required = true) @PathVariable UUID brandId) throws IOException {
        Optional<BrandLogo> logo = brandLogoRepository.findById(brandId);
        if (logo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        storageService.deleteRelativePath(logo.get().getPath());
        brandLogoRepository.deleteById(brandId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "Logo deleted");
        return ResponseEntity.ok(body);
    }
}
