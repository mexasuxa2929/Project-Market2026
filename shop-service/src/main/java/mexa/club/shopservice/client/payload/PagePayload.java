package mexa.club.shopservice.client.payload;

import java.util.List;
import java.util.UUID;

public record PagePayload<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        UUID nextCursor,
        boolean hasMore
) {
    /** Legacy constructor — cursor bo'lmagan (page-based) javoblar uchun */
    public PagePayload(List<T> content, long totalElements, int totalPages, int page, int size) {
        this(content, totalElements, totalPages, page, size, null, false);
    }
}