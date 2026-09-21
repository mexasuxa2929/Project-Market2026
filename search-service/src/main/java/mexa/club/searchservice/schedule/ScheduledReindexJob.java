package mexa.club.searchservice.schedule;

import mexa.club.searchservice.client.ProductServiceClient;
import mexa.club.searchservice.controller.InternalSearchController;
import mexa.club.searchservice.dto.ProductIndexRequest;
import mexa.club.searchservice.service.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledReindexJob {
    private final ProductServiceClient productServiceClient;
    private final SearchService searchService;

    public ScheduledReindexJob(ProductServiceClient productServiceClient, SearchService searchService) {
        this.productServiceClient = productServiceClient;
        this.searchService = searchService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void reindexNightly() {
        List<Map<String, Object>> products = extractContent(productServiceClient.products(0, 1000).data());
        for (Map<String, Object> p : products) {
            searchService.index(new ProductIndexRequest(
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
            ));
        }
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
