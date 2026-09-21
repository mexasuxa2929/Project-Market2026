package mexa.club.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_price_tier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Minimal miqdor (0 dan boshlanadi) */
    @Column(name = "min_qty", nullable = false)
    private int minQty;

    /** Maksimal miqdor; NULL = cheksiz */
    @Column(name = "max_qty")
    private Integer maxQty;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency = "UZS";

    /** RETAIL | WHOLESALE | PURCHASE */
    @Column(name = "price_type", nullable = false, length = 20)
    private String priceType = "RETAIL";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
