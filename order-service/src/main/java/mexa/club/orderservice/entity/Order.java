package mexa.club.orderservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;
    @Column(nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;
    private UUID deliveryAddressId;
    @Column(length = 500)
    private String deliveryAddress;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryFee;
    /**
     * Orderning umumiy yetkazish muddati (kun): itemlarning samarali
     * muddatlari ichidan eng kattasi. Har item uchun: omborda zaxira
     * yetsa → deliveryDaysMin, yetmasa → deliveryDaysMax.
     */
    private Integer estimatedDeliveryDays;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false, length = 8)
    private String currency;
    @Column(length = 1000)
    private String note;
    @Column(length = 500)
    private String cancelReason;
    @Column(length = 1000)
    private String adminNote;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus paymentStatus;
    private UUID paymentId;
    /** To'lov usuli: CASH | CARD | ONLINE (defolt CASH) */
    @Column(length = 32)
    private String paymentMethod;
    @Column(length = 64)
    private String discountCode;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    /** Yopilgan vaqti: DELIVERED/CANCELLED/REFUNDED da set qilinadi, daromad shu sanaga bog'lanadi. */
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (currency == null || currency.isBlank()) {
            currency = "UZS";
        }
        if (deliveryFee == null) {
            deliveryFee = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
