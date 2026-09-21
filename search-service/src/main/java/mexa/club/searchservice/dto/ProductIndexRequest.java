package mexa.club.searchservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductIndexRequest(
        String id,
        String name,
        String barcode,
        String description,
        String categoryId,
        String categoryName,
        String brandId,
        String brandName,
        List<String> tags,
        Double salePrice,
        Boolean active,
        String imageUrl,
        Boolean inStock,
        LocalDateTime createdAt
) {}
