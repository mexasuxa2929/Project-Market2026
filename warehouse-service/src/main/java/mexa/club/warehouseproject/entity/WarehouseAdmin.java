package mexa.club.warehouseproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Omborga biriktirilgan admin foydalanuvchilar — {@code users.id} (auth-service bilan bir xil bazada).
 */
@Entity
@Table(
        name = "warehouse_admin",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class WarehouseAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
