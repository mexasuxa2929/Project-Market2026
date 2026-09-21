package mexa.club.warehouseproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "stock_lot",
        indexes = {
                @Index(name = "idx_stock_lot_wh_product", columnList = "warehouse_id, product_id"),
                @Index(name = "idx_stock_lot_received", columnList = "received_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    private UUID purchaseId;

    @Column(name = "purchase_item_id")
    private UUID purchaseItemId;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockLot)) return false;
        StockLot lot = (StockLot) o;
        return id != null && id.equals(lot.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
