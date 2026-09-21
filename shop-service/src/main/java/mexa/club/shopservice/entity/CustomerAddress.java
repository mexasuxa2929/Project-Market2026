package mexa.club.shopservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shop_customer_address")
@Getter
@Setter
@NoArgsConstructor
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 128)
    private String label;

    /**
     * Manzil nomi/matni (geocode qidiruvdan tanlangan display_name).
     * Viloyat/tuman matn maydonlari olib tashlandi — manzil nuqta (lat/lng)
     * bilan aniqlanadi, matn aralashmasi yo'q.
     */
    @Column(length = 512)
    private String name;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "line2", length = 512)
    private String line2;

    @Column(length = 64)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
