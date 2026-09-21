package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Product;
import mexa.club.productservice.entity.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

/**
 * Dinamik filter: name (contains), barcode (equals), categoryId/brandId (equals).
 */
public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    /**
     * LIKE maxsus belgilarini escape qiladi: % _ \
     */
    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Product> barcodeEquals(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("barcode"), barcode.trim());
    }

    public static Specification<Product> categoryEquals(UUID categoryId) {
        if (categoryId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Product> brandEquals(UUID brandId) {
        if (brandId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("brandId"), brandId);
    }

    public static Specification<Product> manufacturerEquals(UUID manufacturerId) {
        if (manufacturerId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("manufacturerId"), manufacturerId);
    }

    public static Specification<Product> activeEquals(Boolean active) {
        if (active == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> tagEquals(String tag) {
        if (tag == null || tag.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            var join = root.join("tags");
            query.distinct(true);
            return cb.equal(cb.lower(join.get("name")), tag.trim().toLowerCase(Locale.ROOT));
        };
    }

    public static Specification<Product> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        try {
            ProductStatus ps = ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            return (root, query, cb) -> cb.equal(root.get("status"), ps);
        } catch (IllegalArgumentException e) {
            return (root, query, cb) -> cb.conjunction();
        }
    }

    public static Specification<Product> featuredEquals(Boolean featured) {
        if (featured == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("featured"), featured);
    }

    /**
     * Faqat rang guruhi vakillarini (primaryVariant = true) qaytaradi.
     * Ro'yxatda bir guruhdagi rang nusxalari dublikat bo'lib ko'rinmasligi uchun ishlatiladi.
     */
    public static Specification<Product> primaryOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("primaryVariant"));
    }

    /**
     * Keyset (cursor) pagination: berilgan id dan katta bo'lgan qatorlarni qaytaradi.
     * OFFSET'dan farqli o'laroq har bir sahifa uchun index'dan to'g'ridan-to'g'ri o'qiydi.
     */
    public static Specification<Product> afterId(UUID afterId) {
        if (afterId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThan(root.get("id"), afterId);
    }

    public static Specification<Product> combined(
            String name,
            String barcode,
            UUID categoryId,
            UUID brandId,
            UUID manufacturerId,
            Boolean active,
            String tag,
            String status,
            Boolean featured
    ) {
        return Specification.where(nameContains(name))
                .and(barcodeEquals(barcode))
                .and(categoryEquals(categoryId))
                .and(brandEquals(brandId))
                .and(manufacturerEquals(manufacturerId))
                .and(activeEquals(active))
                .and(tagEquals(tag))
                .and(statusEquals(status))
                .and(featuredEquals(featured));
    }
}

