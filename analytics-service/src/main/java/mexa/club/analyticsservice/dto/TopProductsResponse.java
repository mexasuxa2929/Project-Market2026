package mexa.club.analyticsservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TopProductsResponse(String period, List<Item> items) {
    public record Item(int rank, UUID productId, String productName, long soldCount, BigDecimal revenue) {}
}
