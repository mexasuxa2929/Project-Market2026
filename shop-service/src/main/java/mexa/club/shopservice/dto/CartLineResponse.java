package mexa.club.shopservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartLineResponse(
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        Instant updatedAt,
        String productName,
        String imageUrl,
        String color,
        String colorCode
) {
}
