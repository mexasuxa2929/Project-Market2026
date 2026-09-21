package mexa.club.searchservice.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        String query,
        long total,
        int page,
        int size,
        List<?> products,
        Map<String, Object> facets
) {}
