package mexa.club.orderservice.client.payload;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        List<String> imageUrls,
        Integer deliveryDaysMin,
        Integer deliveryDaysMax
) {}
