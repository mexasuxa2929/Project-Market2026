package mexa.club.searchservice.schedule;

import mexa.club.searchservice.client.ProductServiceClient;
import mexa.club.searchservice.dto.ProductIndexRequest;
import mexa.club.searchservice.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledReindexJob {
    private static final Logger log = LoggerFactory.getLogger(ScheduledReindexJob.class);

    private final ProductServiceClient productServiceClient;
    private final SearchService searchService;

    private volatile LocalDateTime lastReindexAt;
    private volatile Integer lastReindexCount;
    private volatile Boolean lastReindexOk;

    public ScheduledReindexJob(ProductServiceClient productServiceClient, SearchService searchService) {
        this.productServiceClient = productServiceClient;
        this.searchService = searchService;
    }

    public LocalDateTime getLastReindexAt() { return lastReindexAt; }
    public Integer getLastReindexCount() { return lastReindexCount; }
    public Boolean getLastReindexOk() { return lastReindexOk; }

    @Scheduled(
            fixedDelayString = "${app.search.reindex-interval-ms:600000}",
            initialDelayString = "${app.search.reindex-initial-delay-ms:30000}")
    public void reindex() {
        runReindex();
    }

    /**
     * To'liq reindex (scheduler va admin endpointdan umumiy).
     * M-27: 1000 hujjat chegarasi olib tashlandi — sahifalab butun katalog indekslanadi.
     * Indekslangan mahsulotlar sonini qaytaradi; xatoda -1 va holat yoziladi.
     */
    public int runReindex() {
        try {
            int pageSize = 1000;
            int page = 0;
            int totalIndexed = 0;
            while (true) {
                Map<String, Object> data = productServiceClient.products(page, pageSize).data();
                List<Map<String, Object>> batch = extractContent(data);
                if (batch.isEmpty()) {
                    break;
                }
                long totalElements = data != null && data.get("totalElements") instanceof Number n
                        ? n.longValue() : (long) (page + 1) * pageSize;
                for (Map<String, Object> p : batch) {
                    searchService.index(mapProduct(p));
                }
                totalIndexed += batch.size();
                page++;
                if ((long) page * pageSize >= totalElements || batch.size() < pageSize) {
                    break;
                }
            }
            lastReindexAt = LocalDateTime.now();
            lastReindexCount = totalIndexed;
            lastReindexOk = true;
            log.debug("Search reindex completed: {} products ({} pages)", totalIndexed, page);
            return totalIndexed;
        } catch (Exception e) {
            lastReindexAt = LocalDateTime.now();
            lastReindexCount = null;
            lastReindexOk = false;
            log.warn("Search reindex failed: {}", e.getMessage());
            return -1;
        }
    }

    private ProductIndexRequest mapProduct(Map<String, Object> p) {
        return new ProductIndexRequest(
                String.valueOf(p.get("id")),
                String.valueOf(p.get("name")),
                String.valueOf(p.getOrDefault("barcode", "")),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("categoryId", "")),
                String.valueOf(p.getOrDefault("categoryName", "")),
                String.valueOf(p.getOrDefault("brandId", "")),
                String.valueOf(p.getOrDefault("brandName", "")),
                List.of(),
                parseDouble(p.get("salePrice")),
                Boolean.TRUE.equals(p.get("active")),
                String.valueOf(p.getOrDefault("imageUrl", "")),
                true,
                LocalDateTime.now()
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Map<String, Object> map) {
        if (map == null) return List.of();
        Object content = map.get("content");
        if (content instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    private Double parseDouble(Object v) {
        if (v == null) return 0d;
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
    }
}
