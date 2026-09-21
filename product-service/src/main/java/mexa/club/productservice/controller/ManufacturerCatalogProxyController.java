package mexa.club.productservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import mexa.club.productservice.service.ReferenceUpstreamProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/manufacturers")
@Tag(name = "Manufacturers", description = "Manufacturer catalog management proxied from the reference data service. All requests are forwarded upstream; returns 503 if the upstream service is unavailable.")
@SecurityRequirement(name = "bearerAuth")
public class ManufacturerCatalogProxyController {

    private final ReferenceUpstreamProxyService upstreamProxyService;

    public ManufacturerCatalogProxyController(ReferenceUpstreamProxyService upstreamProxyService) {
        this.upstreamProxyService = upstreamProxyService;
    }

    private ResponseEntity<Map<String, Object>> unavailable() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "REFERENCE_DATA_UNAVAILABLE");
        error.put("message", "Reference catalog is temporarily unavailable");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", error);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping
    @Operation(
        summary = "List manufacturers",
        description = "Returns a paginated list of manufacturers from the upstream reference data service. Query parameters are forwarded as-is."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manufacturer list returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @ApiResponse(responseCode = "503", description = "Upstream reference catalog service is temporarily unavailable")
    })
    public ResponseEntity<?> list(
            HttpServletRequest request,
            @Parameter(description = "Bearer JWT token forwarded to the upstream service", hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (!upstreamProxyService.isAvailable()) {
            return unavailable();
        }
        return upstreamProxyService.forward(
                "GET",
                "/api/manufacturers",
                request.getQueryString(),
                null,
                null,
                authorization
        );
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get manufacturer by ID",
        description = "Returns a single manufacturer by its UUID from the upstream reference data service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manufacturer found and returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_VIEW authority"),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found for the given ID"),
        @ApiResponse(responseCode = "503", description = "Upstream reference catalog service is temporarily unavailable")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "UUID of the manufacturer", required = true) @PathVariable UUID id,
            HttpServletRequest request,
            @Parameter(description = "Bearer JWT token forwarded to the upstream service", hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (!upstreamProxyService.isAvailable()) {
            return unavailable();
        }
        return upstreamProxyService.forward(
                "GET",
                "/api/manufacturers/" + id,
                request.getQueryString(),
                null,
                null,
                authorization
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Create a manufacturer",
        description = "Forwards a create-manufacturer request to the upstream reference data service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Manufacturer created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @ApiResponse(responseCode = "503", description = "Upstream reference catalog service is temporarily unavailable")
    })
    public ResponseEntity<?> create(
            HttpServletRequest request,
            @Parameter(description = "Bearer JWT token forwarded to the upstream service", hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) throws java.io.IOException {
        if (!upstreamProxyService.isAvailable()) {
            return unavailable();
        }
        byte[] body = request.getInputStream().readAllBytes();
        String contentType = request.getContentType();
        return upstreamProxyService.forward("POST", "/api/manufacturers", null, body, contentType, authorization);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Update a manufacturer",
        description = "Forwards an update-manufacturer request to the upstream reference data service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manufacturer updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request — validation error in the request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found for the given ID"),
        @ApiResponse(responseCode = "503", description = "Upstream reference catalog service is temporarily unavailable")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "UUID of the manufacturer to update", required = true) @PathVariable UUID id,
            HttpServletRequest request,
            @Parameter(description = "Bearer JWT token forwarded to the upstream service", hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) throws java.io.IOException {
        if (!upstreamProxyService.isAvailable()) {
            return unavailable();
        }
        byte[] body = request.getInputStream().readAllBytes();
        String contentType = request.getContentType();
        return upstreamProxyService.forward("PUT", "/api/manufacturers/" + id, null, body, contentType, authorization);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(
        summary = "Delete a manufacturer",
        description = "Forwards a delete-manufacturer request to the upstream reference data service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Manufacturer deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token is missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden — caller lacks PRODUCT_MANAGE authority"),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found for the given ID"),
        @ApiResponse(responseCode = "503", description = "Upstream reference catalog service is temporarily unavailable")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "UUID of the manufacturer to delete", required = true) @PathVariable UUID id,
            @Parameter(description = "Bearer JWT token forwarded to the upstream service", hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (!upstreamProxyService.isAvailable()) {
            return unavailable();
        }
        return upstreamProxyService.forward("DELETE", "/api/manufacturers/" + id, null, null, null, authorization);
    }
}
