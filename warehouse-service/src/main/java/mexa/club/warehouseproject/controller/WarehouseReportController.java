package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.service.WarehouseReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Warehouse Reports", description = "Generate downloadable reports (XLSX/PDF) for stock, purchases and stock movements")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/reports")
public class WarehouseReportController {

    private final WarehouseReportService warehouseReportService;

    public WarehouseReportController(WarehouseReportService warehouseReportService) {
        this.warehouseReportService = warehouseReportService;
    }

    @Operation(summary = "Download stock report", description = "Generates and downloads a current stock snapshot report for the warehouse. Supported formats: xlsx (default), pdf. Requires REPORT_WAREHOUSE authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated and returned as binary file"),
            @ApiResponse(responseCode = "400", description = "Unsupported format value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority — REPORT_WAREHOUSE required"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('REPORT_WAREHOUSE')")
    public ResponseEntity<byte[]> stock(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Output format: xlsx (default) or pdf") @RequestParam(defaultValue = "xlsx") String format
    ) {
        return warehouseReportService.stockReport(warehouseId, format);
    }

    @Operation(summary = "Download purchases report", description = "Generates and downloads a purchases report for the warehouse, optionally filtered by date range. Supported formats: xlsx (default), pdf. Requires REPORT_WAREHOUSE authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated and returned as binary file"),
            @ApiResponse(responseCode = "400", description = "Invalid date format or unsupported format value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority — REPORT_WAREHOUSE required"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('REPORT_WAREHOUSE')")
    public ResponseEntity<byte[]> purchases(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Start date (inclusive), format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (inclusive), format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Output format: xlsx (default) or pdf") @RequestParam(defaultValue = "xlsx") String format
    ) {
        return warehouseReportService.purchasesReport(warehouseId, dateFrom, dateTo, format);
    }

    @Operation(summary = "Download stock movements report", description = "Generates and downloads a stock movements report for the warehouse, optionally filtered by date range and movement type. Supported formats: xlsx (default), pdf. Requires REPORT_WAREHOUSE authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated and returned as binary file"),
            @ApiResponse(responseCode = "400", description = "Invalid date/time format or unsupported format value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient authority — REPORT_WAREHOUSE required"),
            @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('REPORT_WAREHOUSE')")
    public ResponseEntity<byte[]> movements(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Start datetime (inclusive), ISO-8601 format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @Parameter(description = "End datetime (inclusive), ISO-8601 format") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @Parameter(description = "Filter by movement type (e.g. PURCHASE, SALES_OUT, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT)") @RequestParam(required = false) MovementType movementType,
            @Parameter(description = "Output format: xlsx (default) or pdf") @RequestParam(defaultValue = "xlsx") String format
    ) {
        return warehouseReportService.movementsReport(warehouseId, dateFrom, dateTo, movementType, format);
    }
}
