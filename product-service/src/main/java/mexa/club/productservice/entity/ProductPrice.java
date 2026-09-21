package mexa.club.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private LocalDateTime effectiveDate;

    private LocalDateTime endDate;

    /** Chegirma foizi (0..100). Null yoki 0 = chegirma yo'q. */
    private Integer discountPercent;

    /** Chegirma boshlanish sanasi (null = cheklanmagan). */
    private LocalDateTime discountStartDate;

    /** Chegirma tugash sanasi (null = cheklanmagan). Muddat o'tgach chegirma avtomatik o'chadi. */
    private LocalDateTime discountEndDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductPrice)) return false;
        ProductPrice price = (ProductPrice) o;
        return id != null && id.equals(price.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

