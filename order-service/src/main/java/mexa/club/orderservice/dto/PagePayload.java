package mexa.club.orderservice.dto;

import java.util.List;

public record PagePayload<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
