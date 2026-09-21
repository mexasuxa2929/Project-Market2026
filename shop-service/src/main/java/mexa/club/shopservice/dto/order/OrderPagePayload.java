package mexa.club.shopservice.dto.order;

import java.util.List;

public record OrderPagePayload<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
