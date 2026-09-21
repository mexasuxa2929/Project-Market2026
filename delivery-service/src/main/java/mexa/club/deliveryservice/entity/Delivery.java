package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery")
@Getter
@Setter
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID orderId;
    private UUID courierId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryStatus status;
    @Column(nullable = false)
    private UUID fromWarehouseId;
    @Column(nullable = false, length = 600)
    private String deliveryAddress;
    @Column(nullable = false, length = 150)
    private String recipientName;
    @Column(nullable = false, length = 64)
    private String recipientPhone;
    @Column(nullable = false, length = 128)
    private String region;
    @Column(nullable = false, length = 128)
    private String district;
    @Column(nullable = false)
    private LocalDate estimatedDelivery;
    private LocalDateTime actualDelivery;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryFee;
    @Column(nullable = false, unique = true, length = 32)
    private String trackingCode;
    @Column(length = 500)
    private String note;
    @Column(length = 500)
    private String failReason;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
