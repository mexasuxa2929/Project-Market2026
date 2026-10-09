package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Product;
import mexa.club.productservice.entity.ProductPrice;
import mexa.club.productservice.entity.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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

    /**
     * Narx oralig'i: UI'da ko'rinadigan EFFECTIVE narx min..max bo'lgan
     * mahsulotlar (chegirma faol bo'lsa salePrice*(100-d)/100, aks holda salePrice).
     * DB'da filtrlangani uchun paginatsiya/count aniq bo'ladi (M-H6).
     */
    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            var sub = query.subquery(ProductPrice.class);
            var pp = sub.from(ProductPrice.class);
            var match = cb.equal(pp.get("productId"), root.get("id"));

            var sale = pp.<java.math.BigDecimal>get("salePrice");
            var discount = pp.<Integer>get("discountPercent");
            var start = pp.<java.time.LocalDateTime>get("discountStartDate");
            var end = pp.<java.time.LocalDateTime>get("discountEndDate");
            var now = java.time.LocalDateTime.now();

            // Chegirma faol: foiz > 0 va sana oralig'i ichida (null = cheklanmagan)
            var discountActive = cb.and(
                    cb.isNotNull(discount),
                    cb.greaterThan(discount, 0),
                    cb.or(cb.isNull(start), cb.lessThanOrEqualTo(start, now)),
                    cb.or(cb.isNull(end), cb.greaterThanOrEqualTo(end, now))
            );

            // effective = chegirma faol bo'lsa sale*(100-d)/100, aks holda sale
            var numerator = cb.prod(sale, cb.diff(cb.literal(100), cb.coalesce(discount, cb.literal(0))));
            var discounted = cb.function("round", java.math.BigDecimal.class, cb.quot(numerator, cb.literal(100)), cb.literal(2));
            var effective = cb.<java.math.BigDecimal>selectCase()
                    .when(discountActive, discounted)
                    .otherwise(sale);

            var inRange = minPrice != null && maxPrice != null
                    ? cb.between(effective, minPrice, maxPrice)
                    : (minPrice != null
                        ? cb.greaterThanOrEqualTo(effective, minPrice)
                        : cb.lessThanOrEqualTo(effective, maxPrice));

            sub.select(pp).where(cb.and(match, inRange));
            return cb.exists(sub);
        };
    }
}

