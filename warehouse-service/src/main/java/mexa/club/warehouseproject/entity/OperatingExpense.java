package mexa.club.warehouseproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operating_expense", indexes = {
        @Index(name = "idx_operating_expense_wh_date", columnList = "warehouse_id, incurred_at"),
        @Index(name = "idx_operating_expense_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class OperatingExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OperatingExpenseCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    private String currency = "UZS";

    @Column(length = 512)
    private String description;

    @Column(name = "incurred_at", nullable = false)
    private LocalDateTime incurredAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currency == null || currency.isBlank()) currency = "UZS";
    }
}
