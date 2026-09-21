package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByTags_NameIgnoreCase(String tag);

    List<Product> findByGroupIdOrderByColorAsc(UUID groupId);

    /** BATCH: bir nechta guruhdagi barcha mahsulotlar (rang sibling'larini bitta query'da olish uchun). */
    List<Product> findByGroupIdIn(Collection<UUID> groupIds);

    @Query("""
            SELECT p
            FROM Product p
            LEFT JOIN ProductPrice pp
                ON pp.productId = p.id
               AND pp.effectiveDate = (
                    SELECT MAX(pp2.effectiveDate)
                    FROM ProductPrice pp2
                    WHERE pp2.productId = p.id
                      AND (pp2.endDate IS NULL OR pp2.endDate > CURRENT_TIMESTAMP)
               )
            WHERE (:name IS NULL OR TRIM(:name) = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:name), '%')))
              AND (:barcode IS NULL OR TRIM(:barcode) = '' OR p.barcode = TRIM(:barcode))
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:brandId IS NULL OR p.brandId = :brandId)
              AND (:manufacturerId IS NULL OR p.manufacturerId = :manufacturerId)
              AND (:active IS NULL OR p.active = :active)
              AND (:minPrice IS NULL OR pp.salePrice >= :minPrice)
              AND (:maxPrice IS NULL OR pp.salePrice <= :maxPrice)
            """)
    Page<Product> searchWithLatestPrice(
            @Param("name") String name,
            @Param("barcode") String barcode,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("manufacturerId") UUID manufacturerId,
            @Param("active") Boolean active,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}

