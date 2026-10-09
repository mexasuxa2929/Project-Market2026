package mexa.club.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.searchservice.client.ProductServiceClient;
import mexa.club.searchservice.dto.ApiResponse;
import mexa.club.searchservice.repository.ProductSearchRepository;
import mexa.club.searchservice.schedule.ScheduledReindexJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin boshqaruvi (T-H8): qidiruv indeksi holati va qo'lda reindex.
 * Xavfsizlik: `/api/search/admin/**` → PRODUCT_MANAGE (SecurityConfig).
 */
@Tag(name = "Admin - Search Ops", description = "Search index status and manual reindex for the admin panel.")
@RestController
@RequestMapping("/api/search/admin")
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class SearchAdminController {

    private final ProductSearchRepository repository;
    private final ScheduledReindexJob reindexJob;
    private final ProductServiceClient productServiceClient;

    public SearchAdminController(
            ProductSearchRepository repository,
            ScheduledReindexJob reindexJob,
            ProductServiceClient productServiceClient) {
        this.repository = repository;
        this.reindexJob = reindexJob;
        this.productServiceClient = productServiceClient;
    }

    @Operation(
        summary = "Search index status",
        description = "Returns indexed document count, total products from product-service and the last reindex outcome."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status returned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller lacks PRODUCT_MANAGE authority")
    })
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("indexedCount", indexedCount());
        m.put("productTotal", productTotal());
        m.put("lastReindexAt", reindexJob.getLastReindexAt());
        m.put("lastReindexCount", reindexJob.getLastReindexCount());
        m.put("lastReindexOk", reindexJob.getLastReindexOk());
        m.put("serverTime", LocalDateTime.now().toString());
        return ApiResponse.ok(m);
    }

    @Operation(
        summary = "Trigger full reindex",
        description = "Fetches products from product-service and re-indexes them into Elasticsearch immediately (same job as the periodic scheduler)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reindex finished; response includes count (-1 on failure)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller lacks PRODUCT_MANAGE authority")
    })
    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex() {
        int count = reindexJob.runReindex();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reindexedCount", count);
        m.put("ok", count >= 0);
        m.put("finishedAt", LocalDateTime.now().toString());
        return ApiResponse.ok(m);
    }

    private long indexedCount() {
        try {
            return repository.count();
        } catch (Exception e) {
            return -1;
        }
    }

    private Object productTotal() {
        try {
            Map<String, Object> data = productServiceClient.products(0, 1).data();
            return data == null ? null : data.get("totalElements");
        } catch (Exception e) {
            return null;
        }
    }
}
