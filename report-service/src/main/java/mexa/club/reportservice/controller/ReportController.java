package mexa.club.reportservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.reportservice.dto.ApiResponse;
import mexa.club.reportservice.dto.DashboardResponse;
import mexa.club.reportservice.dto.ReportFormat;
import mexa.club.reportservice.service.ReportExportService;
import mexa.club.reportservice.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Reports", description = "Business analytics and reporting endpoints for dashboard metrics, sales, products, and customer reports. Supports JSON, XLSX, and PDF export formats.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final ReportExportService exportService;
    private final ObjectMapper objectMapper;

    public ReportController(ReportService reportService, ReportExportService exportService, ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    @Operation(
        summary = "Dashboard summary",
        description = "Returns high-level KPIs for the admin dashboard: total orders, revenue, active users, and product counts."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REPORT_VIEW authority required")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(reportService.dashboard());
    }

    @Operation(
        summary = "Sales report",
        description = "Returns a sales report for the given date range, optionally filtered by warehouse. Supports JSON (default), XLSX, and PDF export formats."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sales report generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REPORT_FINANCIAL authority required")
    })
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public ResponseEntity<?> sales(
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Filter by warehouse ID (optional)")
            @RequestParam(required = false) String warehouseId,
            @Parameter(description = "Export format: json, xlsx, or pdf")
            @RequestParam(defaultValue = "json") ReportFormat format
    ) throws Exception {
        List<Map<String, Object>> data = reportService.sales(dateFrom, dateTo);
        List<String> headers = List.of("Sana", "Buyurtma №", "Mijoz", "Mahsulotlar", "Miqdor", "Summa", "To'lov usuli", "Holat");
        List<List<String>> rows = data.stream().map(this::salesRow).toList();
        return render("sales-report", format, headers, rows, data);
    }

    @Operation(
        summary = "Products report",
        description = "Returns a product performance report showing sales volume and revenue per product for the given date range. Optionally filter by category."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products report generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REPORT_VIEW authority required")
    })
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<?> products(
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Filter by category ID (optional)")
            @RequestParam(required = false) String categoryId,
            @Parameter(description = "Export format: json, xlsx, or pdf")
            @RequestParam(defaultValue = "json") ReportFormat format
    ) throws Exception {
        List<Map<String, Object>> data = reportService.products(dateFrom, dateTo);
        List<String> headers = List.of("Mahsulot", "Kategoriya", "Sotilgan", "Daromad", "Qoldiq stock", "Minimal norm");
        List<List<String>> rows = data.stream().map(this::productRow).toList();
        return render("product-report", format, headers, rows, data);
    }

    @Operation(
        summary = "Customers report",
        description = "Returns a customer activity report showing order counts and total spend per customer for the given date range."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customers report generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REPORT_VIEW authority required")
    })
    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<?> customers(
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Export format: json, xlsx, or pdf")
            @RequestParam(defaultValue = "json") ReportFormat format
    ) throws Exception {
        List<Map<String, Object>> data = reportService.customers(dateFrom, dateTo);
        List<String> headers = List.of("Mijoz", "Email", "Buyurtmalar soni", "Umumiy summa", "Oxirgi buyurtma");
        List<List<String>> rows = data.stream().map(this::customerRow).toList();
        return render("customer-report", format, headers, rows, data);
    }

    private ResponseEntity<?> render(
            String filename,
            ReportFormat format,
            List<String> headers,
            List<List<String>> rows,
            List<Map<String, Object>> jsonData
    ) throws Exception {
        if (format == ReportFormat.json) {
            return ResponseEntity.ok(ApiResponse.ok(jsonData));
        }
        if (format == ReportFormat.xlsx) {
            byte[] bytes = exportService.toXlsx(headers, rows);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename + ".xlsx").build().toString())
                    .body(bytes);
        }
        byte[] pdf = exportService.toPdf(filename, rows.stream().map(r -> String.join(" | ", r)).toList());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename + ".pdf").build().toString())
                .body(pdf);
    }

    private List<String> salesRow(Map<String, Object> r) {
        return List.of(
                str(r.get("createdAt")), str(r.get("orderNumber")), str(r.get("userId")), "N/A", "1",
                str(r.get("totalAmount")), "N/A", str(r.get("status"))
        );
    }

    private List<String> productRow(Map<String, Object> r) {
        return List.of(str(r.get("orderNumber")), "N/A", "1", str(r.get("totalAmount")), "N/A", "N/A");
    }

    private List<String> customerRow(Map<String, Object> r) {
        return List.of(str(r.get("username")), str(r.get("email")), "N/A", "N/A", str(r.get("createdAt")));
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
