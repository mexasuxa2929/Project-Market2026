package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_zone")
@Getter
@Setter
public class DeliveryZone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 128)
    private String region;
    @Column(nullable = false, length = 128)
    private String district;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;
    @Column(nullable = false)
    private Integer estimatedDays = 1;
}
