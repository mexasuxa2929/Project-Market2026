package mexa.club.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item")
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Column(nullable = false)
    private UUID productId;
    /** Warehouse chosen to fulfill this line at order time; used to target sales-out / reverse calls. */
    private UUID warehouseId;
    @Column(nullable = false, length = 150)
    private String productName;
    @Column(length = 512)
    private String imageUrl;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    /**
     * Shu order uchun omborda topilgan miqdor (backorder mexanizmi).
     * DB ga saqlanmaydi — faqat create oqimida ishlatiladi.
     */
    @Transient
    private Integer coveredQuantity;
}
