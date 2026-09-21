package mexa.club.geoservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geo_zone")
@Getter
@Setter
public class GeoZone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 128)
    private String region;

    @Column(length = 128)
    private String district;

    @Column(columnDefinition = "TEXT")
    private String polygon;

    @Column(length = 32)
    private String centerLat;

    @Column(length = 32)
    private String centerLng;

    private Integer zoomLevel = 12;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false)
    private Integer estimatedDays = 1;

    @Column(nullable = false)
    private String color = "#6366F1";

    @Column(nullable = false)
    private boolean active = true;

    private UUID warehouseId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
