package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.WarehouseAdminsRequest;
import mexa.club.warehouseproject.dto.WarehouseCreateRequest;
import mexa.club.warehouseproject.dto.WarehouseResponse;
import mexa.club.warehouseproject.service.WarehouseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Warehouses", description = "CRUD operations for warehouses and their admin assignments")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Operation(summary = "List warehouses", description = "Returns a paginated list of all warehouses. Requires WAREHOUSE_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of warehouses returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_VIEW required")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_VIEW')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<PagePayload<WarehouseResponse>>> list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        var page = warehouseService.listWarehouses(pageable);
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(PagePayload.of(page)));
    }

    @Operation(summary = "Get warehouse by ID", description = "Returns a single warehouse by its UUID. Requires WAREHOUSE_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_VIEW required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_VIEW')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<WarehouseResponse>> get(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.getWarehouse(id)));
    }

    @Operation(summary = "Create warehouse", description = "Creates a new warehouse. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<WarehouseResponse>> create(
            @Valid @RequestBody WarehouseCreateRequest request
    ) {
        WarehouseResponse created = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mexa.club.warehouseproject.api.ApiResponse.ok(created));
    }

    @Operation(summary = "Update warehouse", description = "Updates an existing warehouse by its UUID. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<WarehouseResponse>> update(
            @Parameter(description = "UUID of the warehouse to update", required = true) @PathVariable UUID id,
            @RequestBody WarehouseCreateRequest request
    ) {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.updateWarehouse(id, request)));
    }

    @Operation(summary = "Get all assigned admin user IDs", description = "Returns UUIDs of all users currently assigned as admins to any warehouse. Requires WAREHOUSE_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of assigned user IDs returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_VIEW required")
    })
    @GetMapping("/admins/assigned")
    @PreAuthorize("hasAuthority('WAREHOUSE_VIEW')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<List<UUID>>> getAllAssignedAdmins() {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.getAllAssignedAdminUserIds()));
    }

    @Operation(summary = "Set warehouse admins", description = "Replaces the entire admin list for a warehouse. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admins updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @PutMapping("/{id}/admins")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<WarehouseResponse>> setAdmins(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID id,
            @Valid @RequestBody WarehouseAdminsRequest request
    ) {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.setWarehouseAdmins(id, request)));
    }

    @Operation(summary = "List warehouse admins", description = "Returns UUIDs of all admins assigned to the warehouse. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin list returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/{id}/admins")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<List<UUID>>> listAdmins(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.listWarehouseAdmins(id)));
    }

    @Operation(summary = "Remove warehouse admin", description = "Removes a specific user from the warehouse admin list. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse or user not found")
    })
    @DeleteMapping("/{id}/admins/{userId}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<mexa.club.warehouseproject.api.ApiResponse<WarehouseResponse>> removeAdmin(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID id,
            @Parameter(description = "UUID of the user to remove from admins", required = true) @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(mexa.club.warehouseproject.api.ApiResponse.ok(warehouseService.removeWarehouseAdmin(id, userId)));
    }

    @Operation(summary = "Delete warehouse", description = "Permanently deletes a warehouse by its UUID. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Warehouse deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the warehouse to delete", required = true) @PathVariable UUID id
    ) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Force delete warehouse", description = "Permanently deletes a warehouse and all related stock/purchase records. Intended for force-majeure situations. Requires WAREHOUSE_MANAGE authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Warehouse and all related data deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — WAREHOUSE_MANAGE required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    public ResponseEntity<Void> forceDelete(
            @Parameter(description = "UUID of the warehouse to forcefully delete", required = true) @PathVariable UUID id
    ) {
        warehouseService.forceDeleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
