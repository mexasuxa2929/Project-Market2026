package mexa.club.productservice.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring {@link Page} ning soddalashtirilgan JSON ko‘rinishi.
 */
public record PagePayload<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public static <T> PagePayload<T> of(Page<T> page) {
        return new PagePayload<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}

