package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.warehouseproject.api.ApiResponse;
import mexa.club.warehouseproject.api.PagePayload;
import mexa.club.warehouseproject.dto.AuditLogResponse;
import mexa.club.warehouseproject.service.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Audit Logs", description = "Query the warehouse audit log trail for tracking changes to stock, purchases, returns and other entities")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Search audit logs", description = "Returns a paginated and filterable list of audit log entries. Supports filtering by entity type, entity ID, and warehouse. Results are sorted by creation time descending by default. Requires AUDIT_LOG_VIEW authority.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit log entries returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient authority — AUDIT_LOG_VIEW required")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ResponseEntity<ApiResponse<PagePayload<AuditLogResponse>>> list(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
            @Parameter(description = "Filter by entity type (e.g. WAREHOUSE, PURCHASE, STOCK_RETURN)") @RequestParam(required = false) String targetType,
            @Parameter(description = "Filter by the UUID of the target entity (as a string)") @RequestParam(required = false) String targetId,
            @Parameter(description = "Filter by the warehouse UUID to which the log entry belongs") @RequestParam(required = false) UUID warehouseId
    ) {
        var page = auditLogService.search(targetType, targetId, warehouseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagePayload.of(page)));
    }
}
