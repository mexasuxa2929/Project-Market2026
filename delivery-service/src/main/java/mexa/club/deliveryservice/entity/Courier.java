package mexa.club.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "courier")
@Getter
@Setter
public class Courier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true)
    private UUID userId;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 64)
    private String phone;
    @Column(nullable = false, length = 128)
    private String region;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Integer currentDeliveries = 0;
}
